package org.example.controller;

// FileController.java

import org.apache.commons.io.FileUtils;
import org.example.model.FileInfo;
import org.springframework.core.io.InputStreamResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.*;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

@RestController
public class FileController {

    // 基础路径应通过配置文件读取
    private static final Path BASE_PATH = Paths.get("E:/study/java/data").normalize().toAbsolutePath();

    @GetMapping("/api/files")
    public ResponseEntity<?> listFiles(
            @RequestParam(required = false) String path  // 移除默认值
    ) {
        try {
            // 处理空路径的情况
            String safePath = (path == null) ? "" : path;

            // 安全解析路径
            Path currentPath = resolveAndValidatePath(safePath,true,true);

            // 获取文件列表
            List<FileInfo> files = Files.list(currentPath)
                    .sorted((p1, p2) -> {
                        // 目录优先排序
                        boolean isDir1 = Files.isDirectory(p1);
                        boolean isDir2 = Files.isDirectory(p2);
                        if (isDir1 != isDir2) {
                            return isDir1 ? -1 : 1;
                        }
                        return p1.getFileName().toString()
                                .compareToIgnoreCase(p2.getFileName().toString());
                    })
                    .map(this::convertToFileInfo)
                    .collect(Collectors.toList());

            return ResponseEntity.ok(files);

        } catch (SecurityException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(e.getMessage());
        } catch (NoSuchFileException e) {
            return ResponseEntity.notFound().build();
        } catch (IOException e) {
            return ResponseEntity.internalServerError().body("Error reading directory");
        } catch (InvalidPathException e) {
            return ResponseEntity.badRequest().body("Invalid path format");
        }
    }

    @DeleteMapping("/api/files")
    public ResponseEntity<?> deleteFile(@RequestParam String path) {
        try {
            Path filePath = resolveAndValidatePath(path,true,true);
            if (Files.isDirectory(filePath)) {
                FileUtils.deleteDirectory(filePath.toFile()); // 处理非空目录
            } else {
                Files.delete(filePath);
            }

            return ResponseEntity.ok().build();
        } catch (SecurityException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(e.getMessage());
        } catch (NoSuchFileException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", "文件不存在"));
        } catch (AccessDeniedException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("error", "权限不足"));
        } catch (IOException e) {
            return ResponseEntity.internalServerError()
                    .body(Map.of("error", "删除失败：" + e.getMessage()));
        }
    }
    @PutMapping("/api/files")
    public ResponseEntity<?> renameFile(
            @RequestBody Map<String, String> requestBody
    ) {
        try {
            // 验证并解析旧路径（必须存在）
            Path source = resolveAndValidatePath(
                    requestBody.get("oldPath"),
                    true,  // 检查存在性
                    true   // 需要读权限
            );

            // 验证并解析新路径（不需要存在）
            Path target = resolveAndValidatePath(
                    requestBody.get("newPath"),
                    false, // 不检查存在性
                    false  // 不需要读权限
            );

            // 检查目标父目录权限
            Path parentDir = target.getParent();
            if (!Files.exists(parentDir)) {
                throw new NoSuchFileException("目标目录不存在");
            }
            if (!Files.isWritable(parentDir)) {
                throw new AccessDeniedException("无写入权限");
            }

            // 检查文件冲突
            if (Files.exists(target)) {
                return ResponseEntity.status(HttpStatus.CONFLICT)
                        .body(Map.of("error", "目标文件已存在"));
            }

            // 执行重命名
            Files.move(source, target);
            return ResponseEntity.ok().build();

        } catch (SecurityException | AccessDeniedException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("error", e.getMessage()));
        } catch (NoSuchFileException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", "路径不存在"));
        } catch (IOException e) {
            return ResponseEntity.internalServerError()
                    .body(Map.of("error", "操作失败: " + e.getMessage()));
        }
    }

    @GetMapping("/api/download")
    public ResponseEntity<Resource> downloadFile(@RequestParam String path) {
        try {
            Path filePath = resolveAndValidatePath(path,true,true);
            if (Files.isDirectory(filePath)) {
                return ResponseEntity.badRequest()
                        .body(null);
            }

            InputStreamResource resource = new InputStreamResource(Files.newInputStream(filePath));
            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION,
                            "attachment; filename=\"" + filePath.getFileName() + "\"")
                    .contentType(MediaType.APPLICATION_OCTET_STREAM)
                    .body(resource);
        } catch (SecurityException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        } catch (NoSuchFileException e) {
            return ResponseEntity.notFound().build();
        } catch (AccessDeniedException e) {
            return ResponseEntity.status(403).build();
        } catch (IOException e) {
            return ResponseEntity.internalServerError().build();
        }
    }

    @GetMapping("/api/downloadDir")
    public ResponseEntity<Resource> downloadDirectory(
            @RequestParam String path
    ) {
        Path dirPath = null;
        List<Path> tempFiles = new ArrayList<>();
        try {
            dirPath = resolveAndValidatePath(path, true, true);
            if (!Files.isDirectory(dirPath)) {
                return ResponseEntity.badRequest().build();
            }

            Path tempDir = Files.createTempDirectory("zip_temp_");
            tempFiles.add(tempDir);
            String safeZipName = "directory_" + UUID.randomUUID() + ".zip";
            Path zipPath = tempDir.resolve(safeZipName);
            tempFiles.add(zipPath);

            try (ZipOutputStream zos = new ZipOutputStream(Files.newOutputStream(zipPath))) {
                addSecureDirectoryToZip(zos, dirPath, dirPath.getFileName().toString());
            }

            Resource resource = new InputStreamResource(Files.newInputStream(zipPath));
            HttpHeaders headers = new HttpHeaders();
            headers.add(HttpHeaders.CONTENT_DISPOSITION,
                    "attachment; filename=\"" + dirPath.getFileName() + ".zip\"");

            return ResponseEntity.ok()
                    .headers(headers)
                    .contentType(MediaType.APPLICATION_OCTET_STREAM)
                    .body(resource);

        } catch (SecurityException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        } catch (IOException e) {
            return ResponseEntity.internalServerError().build();
        } finally {
            cleanTempResources(tempFiles);
        }
    }

    // 创建目录接口
    @PostMapping("/api/files")
    public ResponseEntity<?> createDirectory(
            @RequestBody Map<String, String> request
    ) throws IOException {
        String path = request.get("path");
        Path dirPath = resolveAndValidatePath(path, false, false);

        if (Files.exists(dirPath)) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(Map.of("error", "目录已存在"));
        }

        Files.createDirectories(dirPath);
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }


    @PostMapping(value = "/api/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> uploadFiles(
            @RequestParam String path,
            @RequestParam(required = false) String folderName,
            @RequestParam("files") List<MultipartFile> files
    ) throws IOException {
        Path targetDir = resolveAndValidatePath(path, true, true);

        // 处理文件夹冲突
        if (folderName != null) {
            Path folderPath = targetDir.resolve(folderName);
            if (Files.exists(folderPath)) {
                return ResponseEntity.status(HttpStatus.CONFLICT)
                        .body(Map.of("error", "文件夹已存在"));
            }
        }

        files.forEach(file -> {
            String originalFilename = file.getOriginalFilename();
            if (originalFilename == null || originalFilename.isEmpty()) {
                throw new IllegalArgumentException("文件名不能为空");
            }

            // 构造完整路径
            Path filePath = buildSafePath(targetDir, originalFilename);

            try {
                Files.createDirectories(filePath.getParent());
                try (InputStream is = file.getInputStream()) {
                    Files.copy(is, filePath, StandardCopyOption.REPLACE_EXISTING);
                }
            } catch (IOException e) {
                throw new RuntimeException("文件上传失败: " + e.getMessage());
            }
        });

        return ResponseEntity.ok().build();
    }

    private Path buildSafePath(Path baseDir, String relativePath) {
        // 路径标准化处理
        Path resolvedPath = baseDir.resolve(relativePath)
                .normalize()
                .toAbsolutePath();

        // 安全验证
        if (!resolvedPath.startsWith(baseDir)) {
            throw new SecurityException("非法文件路径: " + relativePath);
        }

        // 过滤非法字符（根据系统调整）
        String sanitizedPath = relativePath.replaceAll("[<>:\"|?*]", "_");
        return baseDir.resolve(sanitizedPath);
    }

    private void addSecureDirectoryToZip(ZipOutputStream zos, Path dir, String baseDir) throws IOException {
        Files.walkFileTree(dir, new SimpleFileVisitor<Path>() {
            @Override
            public FileVisitResult preVisitDirectory(Path dirPath, BasicFileAttributes attrs) throws IOException {
                // 跳过根目录，避免在ZIP中创建多余的根目录条目
                if (!dirPath.equals(dir)) {
                    String relativeDir = dir.relativize(dirPath).toString().replace("\\", "/");
                    if (!isSafeFileName(relativeDir)) {
                        return FileVisitResult.SKIP_SUBTREE;
                    }
                    ZipEntry entry = new ZipEntry(baseDir + "/" + relativeDir + "/");
                    zos.putNextEntry(entry);
                    zos.closeEntry();
                }
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                if (Files.isSymbolicLink(file) || Files.isHidden(file) || !Files.isReadable(file)) {
                    return FileVisitResult.CONTINUE;
                }

                String relativePath = dir.relativize(file).toString().replace("\\", "/");
                if (!isSafeFileName(relativePath)) {
                    return FileVisitResult.CONTINUE;
                }

                try {
                    ZipEntry entry = new ZipEntry(baseDir + "/" + relativePath);
                    zos.putNextEntry(entry);
                    Files.copy(file, zos);
                    zos.closeEntry();
                } catch (AccessDeniedException e) {
                    System.err.println("访问被拒绝，跳过文件: " + file);
                }
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult visitFileFailed(Path file, IOException exc) throws IOException {
                if (exc instanceof AccessDeniedException) {
                    System.err.println("访问被拒绝，跳过目录: " + file);
                    return FileVisitResult.SKIP_SUBTREE;
                }
                return super.visitFileFailed(file, exc);
            }
        });
    }

    /**
     * 安全文件名检测
     */
    private boolean isSafeFileName(String fileName) {
        return fileName.matches("[a-zA-Z0-9_\\-./]+"); // 白名单字符集
    }

    /**
     * 强化清理方法
     */
    private void cleanTempResources(List<Path> tempFiles) {
        tempFiles.forEach(path -> {
            try {
                if (Files.isDirectory(path)) {
                    // 递归删除目录
                    Files.walk(path)
                            .sorted(Comparator.reverseOrder())
                            .forEach(p -> {
                                try {
                                    Files.delete(p);
                                } catch (IOException e) {
                                    System.err.println("删除失败: " + p);
                                }
                            });
                } else {
                    Files.deleteIfExists(path);
                }
            } catch (IOException e) {
                System.err.println("清理失败: " + path);
            }
        });
    }

    // 统一路径验证方法
    private Path resolveAndValidatePath(String userInput, boolean checkExists, boolean needRead)
            throws IOException, SecurityException {
        try {
            // 1. URL解码
            String decodedPath = URLDecoder.decode(userInput, StandardCharsets.UTF_8);

            // 2. 过滤危险字符
            if (decodedPath.matches(".*(\\.\\./|%2e%2e/|\\.\\.\\\\|%2e%2e\\\\).*")) {
                throw new SecurityException("路径包含非法字符");
            }

            // 3. 解析路径
            Path resolvedPath = BASE_PATH.resolve(decodedPath).normalize();

            // 4. 严格路径归属验证
            if (!resolvedPath.startsWith(BASE_PATH)) {
                throw new SecurityException("禁止访问外部路径");
            }

            // 5. 存在性检查
            if (checkExists && !Files.exists(resolvedPath)) {
                throw new NoSuchFileException("路径不存在");
            }

            // 6. 权限检查
            if (needRead && !Files.isReadable(resolvedPath)) {
                throw new AccessDeniedException("无读取权限");
            }

            return resolvedPath;
        } catch (InvalidPathException e) {
            throw new SecurityException("非法路径格式");
        }
    }

    // 检测路径穿越模式
    private boolean containsTraversalPattern(String path) {
        // 匹配以下模式：
        // ../
        // ..\
        // URL编码后的变种（如%2e%2e%2f）
        return path.matches(".*(\\.\\./|%2e%2e/|\\.\\.\\\\|%2e%2e\\\\).*");
    }
    // 路径验证方法
    private Path validatePath(String relativePath) throws IOException {
        Path base = Paths.get("E:/study/java/code/file/data");
        Path resolved;

        try {
            resolved = base.resolve(relativePath).normalize();
        } catch (InvalidPathException e) {
            throw new AccessDeniedException("非法路径格式");
        }

        // 防止路径遍历
        if (!resolved.startsWith(base)) {
            throw new AccessDeniedException("禁止访问外部路径");
        }

        // 检查文件是否存在
        if (!Files.exists(resolved)) {
            throw new NoSuchFileException("路径不存在");
        }

        // 检查读写权限
        if (!Files.isReadable(resolved)) {
            throw new AccessDeniedException("无读取权限");
        }

        return resolved;
    }

    private FileInfo convertToFileInfo(Path path) {
        try {
            boolean isDir = Files.isDirectory(path);
            return new FileInfo(
                    path.getFileName().toString(),
                    isDir,
                    formatSize(isDir ? 0 : Files.size(path)),
                    Files.getLastModifiedTime(path).toMillis()
            );
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private String formatSize(long bytes) {
        if (bytes == 0) return "0";
        String[] units = {"B", "KB", "MB", "GB"};
        int unitIndex = (int) (Math.log10(bytes) / 3);
        double value = bytes / Math.pow(1024, unitIndex);
        return String.format("%.1f%s", value, units[unitIndex]);
    }

    // 设置允许访问的基础路径（按需修改）
    private String getBasePath() {
        return "E:/study/java/code/file/data";
    }

}
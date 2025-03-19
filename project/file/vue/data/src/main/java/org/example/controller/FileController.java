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
import org.springframework.web.servlet.mvc.method.annotation.ResponseBodyAdvice;

import java.io.*;
import java.nio.file.*;
import java.nio.file.attribute.*;
import java.util.*;
import java.util.stream.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

@RestController
public class FileController {

    @GetMapping("/api/files")
    public ResponseEntity<?> listFiles(
            @RequestParam(required = false, defaultValue = "./") String path
    ) {
        try {
            Path basePath = Paths.get(getBasePath()).normalize();
            Path currentPath = basePath.resolve(path).normalize().toAbsolutePath();


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
        } catch (NoSuchFileException e) {
            return ResponseEntity.notFound().build();
        } catch (IOException e) {
            return ResponseEntity.internalServerError().body("Error reading directory");
        } catch (InvalidPathException e) {
            return ResponseEntity.badRequest().body("Invalid path format");
        }
    }

    @DeleteMapping("/api/files")
    public ResponseEntity<?> deleteFile(
            @RequestParam String path
    ) {
        try {
            Path filePath = validatePath(path);

            if (Files.isDirectory(filePath)) {
                FileUtils.deleteDirectory(filePath.toFile()); // 处理非空目录
            } else {
                Files.delete(filePath);
            }

            return ResponseEntity.ok().build();

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
    @GetMapping("/api/download")
    public ResponseEntity<Resource> downloadFile(
            @RequestParam String path
    ) {
        try {
            Path filePath = validatePath(path);

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
    ) throws IOException {
        Path dirPath = validatePath(path);

        if (!Files.isDirectory(dirPath)) {
            throw new IllegalArgumentException("路径不是目录");
        }

        // 创建临时ZIP文件
        Path zipPath = Files.createTempFile("directory_", ".zip");
        try (ZipOutputStream zos = new ZipOutputStream(Files.newOutputStream(zipPath))) {
            addDirectoryToZip(zos, dirPath, dirPath.getFileName().toString());
        }

        // 设置响应
        Resource resource = new InputStreamResource(Files.newInputStream(zipPath));
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"" + dirPath.getFileName() + ".zip\"")
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .body(resource)
                .header("X-Temp-File", zipPath.toString()); // 记录临时文件路径
    }

    private void addDirectoryToZip(ZipOutputStream zos, Path dir, String baseDir) throws IOException {
        Files.walk(dir)
                .filter(path -> !Files.isDirectory(path))
                .forEach(path -> {
                    try {
                        String entryName = baseDir + "/" + dir.relativize(path).toString();
                        ZipEntry entry = new ZipEntry(entryName);
                        zos.putNextEntry(entry);
                        Files.copy(path, zos);
                        zos.closeEntry();
                    } catch (IOException e) {
                        throw new UncheckedIOException(e);
                    }
                });
    }

//    // 添加响应完成后的清理逻辑（可选）
//    @ControllerAdvice
//    public static class FileCleanupAdvice implements ResponseBodyAdvice<Object> {
//        @Override
//        public boolean supports(MethodParameter returnType, Class converterType) {
//            return true;
//        }
//
//        @Override
//        public Object beforeBodyWrite(Object body, MethodParameter returnType,
//                                      MediaType selectedContentType, Class selectedConverterType,
//                                      ServerHttpRequest request, ServerHttpResponse response) {
//            if (response.getHeaders().containsKey("X-Temp-File")) {
//                String tempFile = response.getHeaders().getFirst("X-Temp-File");
//                new File(tempFile).delete();
//            }
//            return body;
//        }
//    }

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
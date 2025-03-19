<template>
    <div class="file-manager">
      <!-- 面包屑导航和操作按钮 -->
      <div class="header-container">
        <el-breadcrumb separator="/" class="breadcrumb">
          <el-breadcrumb-item 
            v-for="(path, index) in currentPathSegments" 
            :key="index"
            @click="navigateTo(index)"
          >
            {{ path || 'Root' }}
          </el-breadcrumb-item>
        </el-breadcrumb>
        
        <div class="action-buttons">
          <el-button type="primary" plain @click="createNewFolder">
            <el-icon><FolderAdd /></el-icon>新建文件夹
          </el-button>
          <el-dropdown split-button type="primary" @click="triggerFileUpload">
            上传
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item @click="triggerFileUpload">上传文件</el-dropdown-item>
                <el-dropdown-item 
                  @click="triggerFolderUpload" 
                  :disabled="!isFolderUploadSupported"
                >
                  上传文件夹
                </el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
          <input 
            ref="fileInput" 
            type="file" 
            hidden 
            multiple 
            @change="handleFileUpload"
          >
          <input 
            ref="folderInput" 
            type="file" 
            hidden 
            webkitdirectory 
            @change="handleFolderUpload"
          >
        </div>
      </div>
      <!-- 右键菜单 -->
      <div 
        v-show="contextMenu.visible" 
        class="context-menu"
        :style="{
          left: contextMenu.x + 'px',
          top: contextMenu.y + 'px'
        }"
      >
        <div class="menu-item" @click="handleDownload">下载</div>
        <div class="menu-item" @click="handleRename">重命名</div>
        <div class="menu-item danger" @click="handleDelete">删除</div>
      </div>
      
  
      <!-- 文件列表 -->
      
      <div class="table-container">
        <el-table 
          :data="fileList" 
          style="width: 100%"
          v-loading="loading"
          @row-dblclick="handleDoubleClick"
          @row-contextmenu="handleContextMenu"
        >
          <el-table-column prop="name" label="名称" min-width="240" sortable>
            <template #default="{ row }">
              <div class="file-name-container">
                <el-icon v-if="row.isDirectory" class="file-icon">
                  <Folder />
                </el-icon>
                <el-icon v-else class="file-icon">
                  <Document />
                </el-icon>
                <span class="file-name">{{ row.name }}</span>
              </div>
            </template>
          </el-table-column>

          <el-table-column label="大小" min-width="120" sortable>
            <template #default="{ row }">
              {{ row.isDirectory ? '-' : row.size }}
            </template>
          </el-table-column>
          <el-table-column prop="modifiedTime" label="修改时间" min-width="180" sortable/>
        </el-table>
      </div>
    </div>
  </template>
  
  <script setup>
  import { ref, computed, onMounted, watch } from 'vue'
  import { Document, Folder, FolderAdd, Upload } from '@element-plus/icons-vue'
  import request from '../utils/request'
  import { ElMessage, ElMessageBox } from 'element-plus'
  
  const loading = ref(false)
  const currentPath = ref('.')
  const fileList = ref([])
  const fileInput = ref(null)
  const folderInput = ref(null)
  
  // 新增右键菜单相关状态
  const contextMenu = ref({
    visible: false,
    x: 0,
    y: 0,
    selectedFile: null
  })
  
  // 处理右键点击事件
  const handleContextMenu = (row, column, event) => {
    event.preventDefault()
    contextMenu.value = {
      visible: true,
      x: event.clientX,
      y: event.clientY,
      selectedFile: row
    }
    
    // 点击其他区域关闭菜单
    document.addEventListener('click', closeContextMenu)
  }
  
  // 关闭右键菜单
  /**
   * 关闭右键菜单的处理函数
   * 将右键菜单的可见性设置为 false，并移除点击事件监听器
   * @private
   */
  const closeContextMenu = () => {
    contextMenu.value.visible = false
    document.removeEventListener('click', closeContextMenu)
  }
  
  // 处理下载
  const handleDownload = async () => {
    try {
      const { selectedFile } = contextMenu.value
      if (!selectedFile) return
    
        if (selectedFile.isDirectory){
          const response = await request.get('/api/downloadDir', {
          params: {
            path: `${currentPath.value}/${selectedFile.name}`
          },
          responseType: 'blob'
        });
  
        // 创建下载链接
        const url = window.URL.createObjectURL(new Blob([response.data]));
        const link = document.createElement('a');
        link.href = url;
        link.setAttribute('download', `${selectedFile.name}.zip`);
        document.body.appendChild(link);
        link.click();
        
        // 清理资源
        URL.revokeObjectURL(url);
        document.body.removeChild(link);
        
        ElMessage.success('目录下载已开始');
        return
      }
      
  
      // 调用下载API
      const response = await request.get(`/api/download`, {
        params: {
          path: `${currentPath.value}/${selectedFile.name}`
        },
        responseType: 'blob'
      })
  
      // 创建临时链接下载
      const url = window.URL.createObjectURL(new Blob([response.data]))
      const link = document.createElement('a')
      link.href = url
      link.setAttribute('download', selectedFile.name)
      document.body.appendChild(link)
      link.click()
      document.body.removeChild(link)
      
      ElMessage.success('下载已开始')
    } catch (error) {
      ElMessage.error('下载失败: ' + error.message)
    } finally {
      closeContextMenu()
    }
  }
  
  // 处理删除
  const handleDelete = async () => {
    try {
      const { selectedFile } = contextMenu.value
      if (!selectedFile) return
  
      await ElMessageBox.confirm(
        `确定要永久删除 "${selectedFile.name}" 吗？`,
        '警告',
        {
          confirmButtonText: '确定',
          cancelButtonText: '取消',
          type: 'warning'
        }
      )
  
      // 调用删除API
      await request.delete(`/api/files`, {
        params: {
          path: `${currentPath.value}/${selectedFile.name}`
        }
      })
  
      ElMessage.success('删除成功')
      fetchFiles() // 刷新列表
    } catch (error) {
      handleApiError(error, '删除文件失败')
    } finally {
      closeContextMenu()
    }
  }
  const handleRename = async () => {
    try {
      const { selectedFile } = contextMenu.value
      if (!selectedFile) return
  
      // 弹出输入框获取新文件名
      const { value: newName } = await ElMessageBox.prompt(
        '请输入新文件名',
        '重命名',
        {
          confirmButtonText: '确定',
          cancelButtonText: '取消',
          inputPattern: /^[^\\\/:\*\?"<>\|]+$/, // 过滤非法字符
          inputErrorMessage: '文件名包含非法字符'
        }
      )
  
      if (!newName) return
  
      // 调用重命名API
      await request.put('/api/files', {
        oldPath: `${currentPath.value}/${selectedFile.name}`,
        newPath: `${currentPath.value}/${newName}`
      })
  
      ElMessage.success('重命名成功')
      fetchFiles() // 刷新列表
    } catch (error) {
      if (error !== 'cancel') {
        ElMessage.error(`重命名失败: ${error.response?.data?.message || error.message}`)
      }
    } finally {
      closeContextMenu()
    }
  }
  // 将路径转换为面包屑数组
  const currentPathSegments = computed(() => {
    return currentPath.value.split('/').filter(p => p)
  })
  
  // 获取文件列表
  const fetchFiles = async () => {
    try {
      loading.value = true
      const response = await request.get('/api/files', {
        params: { 
          path: currentPath.value === '.' ? '' : currentPath.value
        }
      })
      
      // 添加数据校验
      if (!Array.isArray(response.data)) {
        throw new Error('Invalid response format')
      }
      
      fileList.value = response.data.map(item => ({
        name: item.name || 'Unnamed',
        isDirectory: !!item.isDirectory,
        size: item.size || '0',
        modifiedTime: formatDate(item.modifiedTime)
      }))
    } catch (error) {
      handleApiError(error, '加载文件列表失败')
    } finally {
      loading.value = false
    }
  }
  
  // 格式化日期
  const formatDate = (timestamp) => {
    try {
      const date = new Date(Number(timestamp))
      return date.toLocaleDateString('zh-CN', {
        year: 'numeric',
        month: '2-digit',
        day: '2-digit',
        hour: '2-digit',
        minute: '2-digit',
        second: '2-digit'
      })
    } catch (e) {
      return 'Invalid Date'
    }
  }
  
  // 处理目录导航
  const navigateTo = (index) => {
    const newPath = currentPathSegments.value.slice(0, index + 1).join('/')
    if (newPath !== currentPath.value) {
      currentPath.value = newPath
    }
  }
  
  // 处理行点击
  const handleDoubleClick = (row) => {
    if (row.isDirectory) {
      // 处理路径拼接
      const newPath = currentPath.value === './' 
        ? row.name 
        : `${currentPath.value}/${row.name}`
      
      currentPath.value = newPath
    }
  }
  
  // 监听路径变化自动刷新
  watch(currentPath, () => {
    fetchFiles()
  })
  
  // 初始化加载
  onMounted(() => {
    fetchFiles()
  })
  
  // 新建文件夹方法
  const createNewFolder = async () => {
    try {
      const newFolderName = '新建文件夹'
      const response = await request.post('/api/files', {
        path: `${currentPath.value}/${newFolderName}`,
        isDirectory: true
      })
      
      if (response.status === 201) {
        ElMessage.success('文件夹创建成功')
        fetchFiles()
      }
    } catch (error) {
      handleApiError(error, '创建文件夹失败')
    }
  }
  
  // 触发文件上传
  const triggerFileUpload = () => {
    fileInput.value.click()
  }
  
  // 检测文件夹上传支持性
  const isFolderUploadSupported = computed(() => {
    return 'webkitdirectory' in document.createElement('input')
  })
  // 触发文件夹上传
  const triggerFolderUpload = () => {
    if (!isFolderUploadSupported.value) {
      ElMessage.warning('当前浏览器不支持文件夹上传')
      return
    }
    folderInput.value.click()
  }
  // 处理文件上传
  const handleFileUpload = async (event) => {
    const files = Array.from(event.target.files)
    if (files.length === 0) return
  
    const formData = new FormData()
    files.forEach(file => {
      formData.append('files', file)
    })
  
    try {
      await request.post('/api/upload', formData, {
        params: { path: currentPath.value },
        headers: { 'Content-Type': 'multipart/form-data' }
      })
      ElMessage.success(`成功上传 ${files.length} 个文件`)
      fetchFiles()
    } catch (error) {
      handleApiError(error, '文件上传失败')
    } finally {
      event.target.value = ''
    }
  }
  const handleFolderUpload = async (event) => {
    const files = Array.from(event.target.files)
    if (files.length === 0) return
  
    const formData = new FormData()
    const folderName = getTopLevelFolderName(files[0].webkitRelativePath)
  
    files.forEach(file => {
      // 保留完整相对路径
      const relativePath = file.webkitRelativePath
      formData.append('files', file, relativePath)
    })
  
    try {
      await request.post('/api/upload', formData, {
        params: { 
          path: currentPath.value,
          folderName: folderName // 传递顶层目录名称
        },
        headers: { 
          'Content-Type': 'multipart/form-data'
        }
      })
      
      ElMessage.success(`成功上传文件夹 "${folderName}"`)
      fetchFiles()
    } catch (error) {
      handleUploadError(error, folderName)
    } finally {
      event.target.value = ''
    }
  }
  
  // 获取顶层目录名称
  const getTopLevelFolderName = (relativePath) => {
    return relativePath.split('/')[0]
  }
  // 错误处理
  const handleUploadError = (error, folderName) => {
    let message = '上传失败'
    if (error.response) {
      switch (error.response.status) {
        case 403:
          message = '权限不足'; break
        case 409:
          message = `文件夹 "${folderName}" 已存在`; break
        default:
          message = error.response.data?.error || message
      }
    }
    ElMessage.error(`${message}: ${error.message}`)
  }
  // 统一API错误处理
  const handleApiError = (error, defaultMessage = '操作失败') => {
    let message = defaultMessage
    if (error.response) {
      // 处理有HTTP响应但状态码非2xx的情况
      switch (error.response.status) {
        case 400:
          message = error.response.data?.error || '请求参数错误'
          break
        case 401:
          message = '请先登录'
          break
        case 403:
          message = error.response.data?.error || '权限不足'
          break
        case 404:
          message = error.response.data?.error || '资源不存在'
          break
        case 409:
          message = error.response.data?.error || '资源冲突'
          break
        case 413:
          message = '文件大小超过限制'
          break
        case 500:
          message = '服务器内部错误，请稍后再试'
          break
        default:
          message = error.response.data?.error || `未知错误 (${error.response.status})`
      }
    } else if (error.request) {
      // 请求已发送但无响应
      message = '服务器无响应，请检查网络连接'
    } else {
      // 请求未发出（如跨域问题、取消请求等）
      message = error.message || '请求配置错误'
    }
    
    // 显示错误提示（可根据需要调整）
    ElMessage.error(message)
    
    // 返回错误信息供后续处理
    return message
  }
  </script>
  
  <style scoped>
  
  /* 添加双击视觉反馈 */
  .el-table__row {
    cursor: pointer;
    transition: background-color 0.2s;
  }
  
  .el-table__row:hover {
    background-color: #f5f7fa;
  }
  
  .el-table__row.double-click-active {
    background-color: #e8f4ff;
  }
  
  .header-container {
    display: flex;
    justify-content: space-between;
    align-items: center;
    margin-bottom: 20px;
    gap: 16px;
  }
  
  .action-buttons {
    display: flex;
    gap: 12px;
    flex-shrink: 0;
  }
  
  .el-button {
    padding: 8px 15px;
  }
  
  .context-menu {
    position: fixed;
    z-index: 9999;
    background: #fff;
    box-shadow: 0 2px 12px 0 rgba(0,0,0,.1);
    border-radius: 4px;
    padding: 5px 0;
  }
  
  .menu-item {
    padding: 8px 20px;
    cursor: pointer;
    font-size: 14px;
    color: #606266;
  }
  
  .menu-item:hover {
    background: #f5f7fa;
    color: var(--el-color-primary);
  }
  
  .danger {
    color: #f56c6c;
  }
  
  .danger:hover {
    color: #f56c6c;
    background: #fef0f0;
  }
  .file-manager {
    padding: 20px;
    max-width: 1200px;
    margin: 0 auto;
  }
  
  .breadcrumb {
    margin-bottom: 20px;
  }
  
  .file-icon {
    vertical-align: middle;
    margin-right: 8px;
  }
  
  .el-breadcrumb-item:hover {
    cursor: pointer;
    color: var(--el-color-primary);
  }
  </style>
  
  
  
  
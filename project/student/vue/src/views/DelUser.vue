<template>
  <div>
    <!-- 精简搜索栏（仅ID查询） -->
    <el-card class="search-bar" shadow="never">
      <div class="delete-search-container">
        <el-input 
          v-model.number="deleteId" 
          placeholder="请输入用户ID" 
          type="number"
          min="1"
          clearable
          style="width: 300px; margin-right: 10px;"
          @keyup.enter="handleQuery"
        />
        <el-button 
          type="primary" 
          @click="handleQuery"
          :loading="loading"
        >
          查询
        </el-button>
        <el-button 
          type="danger" 
          @click="handleDelete"
          :disabled="!userInfo"
          :loading="deleting"
        >
          删除
        </el-button>
      </div>
    </el-card>

    <!-- 用户信息展示 -->
    <el-card v-if="userInfo" class="user-info-card">
      <div slot="header" class="clearfix">
        <span>用户信息确认</span>
      </div>
      <el-descriptions :column="2" border>
        <el-descriptions-item label="ID">{{ userInfo.id }}</el-descriptions-item>
        <el-descriptions-item label="姓名">{{ userInfo.name || '-' }}</el-descriptions-item>
        <el-descriptions-item label="性别">{{ formatSex(userInfo.sex) }}</el-descriptions-item>
        <el-descriptions-item label="电话">{{ userInfo.phone || '-' }}</el-descriptions-item>
        <el-descriptions-item label="生日">{{ userInfo.birthday }}</el-descriptions-item>
        <el-descriptions-item label="学历">{{ formatEducation(userInfo.education) }}</el-descriptions-item>
        <el-descriptions-item label="邮箱">{{ userInfo.email || '-' }}</el-descriptions-item>
        <el-descriptions-item label="地址">{{ userInfo.addr || '-' }}</el-descriptions-item>
        <el-descriptions-item label="创建时间">{{ userInfo.create_time }}</el-descriptions-item>
        <el-descriptions-item label="最后修改时间">{{ userInfo.last_time }}</el-descriptions-item>
      </el-descriptions>
    </el-card>
  </div>
</template>


<script setup>
import { ref } from 'vue'
import { ElMessage } from 'element-plus'
import request from '../utils/request'

// 响应式数据
const deleteId = ref('')
const userInfo = ref(null)
const loading = ref(false)
const deleting = ref(false)

// 查询用户信息
const handleQuery = async () => {
  if (!deleteId.value) {
    ElMessage.warning('请输入用户ID')
    return
  }

  try {
    loading.value = true
    const response = await request.get('/api/GetUser', {
      params: { page: 1, pageSize: 1, id: deleteId.value }
    })

    if (response.data?.code === 200) {
      userInfo.value = response.data.data?.[0] || null
      if (!userInfo.value) {
        ElMessage.warning('用户不存在')
      }
    } else {
      ElMessage.warning(response.data?.message || '用户不存在')
      userInfo.value = null
    }
  } catch (error) {
    handleError(error)
    userInfo.value = null
  } finally {
    loading.value = false
  }
}

// 删除用户
const handleDelete = async () => {
  try {
    deleting.value = true
    const response = await request.delete('/api/DelUser', {
      data: { id: deleteId.value }
    })

    if (response.data?.code === 200) {
      ElMessage.success('删除成功')
      userInfo.value = null
      deleteId.value = ''
    } else {
      ElMessage.error(response.data?.message || '删除失败')
    }
  } catch (error) {
    handleError(error)
  } finally {
    deleting.value = false
  }
}

// 错误处理
const handleError = (error) => {
  let message = '操作失败'
  if (error.response) {
    switch (error.response.status) {
      case 400:
        message = error.response.data?.message || '请求参数错误'
        break
      case 404:
        message = '用户不存在'
        break
      case 500:
        message = '服务器内部错误'
        break
    }
  } else if (error.request) {
    message = error.code === 'ECONNABORTED' ? '请求超时' : '网络连接异常'
  }
  ElMessage.error(message)
  console.error('错误详情:', error)
}

// 格式化函数
const formatSex = (sex) => {
  const map = { M: '男', F: '女' }
  return sex ? map[sex] || '未知' : '-'
}

const formatEducation = (edu) => {
  const map = {
    PHD: '博士',
    MASTER: '硕士',
    BACHELOR: '本科',
    ASSOCIATE: '大专',
    HIGHSCHOOL: '高中',
    1: '高中',
    2: '大专',
    3: '本科',
    4: '硕士',
    5: '博士'
  }
  return edu ? map[edu] || edu : '-'
}
</script>

<style scoped>
.delete-search-container {
  display: flex;
  align-items: center;
}

.user-info-card {
  margin-top: 20px;
}

.el-descriptions {
  margin-top: 15px;
}

.el-descriptions-item__label {
  width: 120px;
  background-color: #f5f7fa;
}
</style>
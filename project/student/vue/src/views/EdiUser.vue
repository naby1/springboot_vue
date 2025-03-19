<template>
    <div>
      <!-- ID 查询栏 -->
      <el-card class="search-bar" shadow="never">
        <div class="edit-search-container">
          <el-input 
            v-model.number="editId" 
            placeholder="请输入要修改的用户ID" 
            type="number"
            min="1"
            clearable
            style="width: 300px; margin-right: 10px;"
            @keyup.enter.native="handleQuery"
          />
          <el-button 
            type="primary" 
            @click="handleQuery"
            :loading="loading"
          >
            查询
          </el-button>
          <el-button 
            @click="resetForm"
          >
            重置
          </el-button>
        </div>
      </el-card>
  
      <!-- 用户信息编辑表单 -->
      <el-card v-if="formData.id" class="edit-form-card">
        <div slot="header" class="clearfix">
          <span>用户信息编辑</span>
        </div>
        <el-form 
          :model="formData" 
          ref="editForm" 
          label-width="100px"
        >
          <!-- 基础信息 -->
          <el-form-item label="ID" prop="id">
            <el-input v-model="formData.id" disabled />
          </el-form-item>
          
          <el-form-item label="姓名" prop="name">
            <el-input v-model.trim="formData.name" />
          </el-form-item>
          
          <el-form-item label="密码" prop="passwd">
            <el-input v-model.trim="formData.passwd"
              v-model="formData.passwd" 
              type="password"
              placeholder="请输入登录密码"
            />
          </el-form-item>

          <el-form-item label="生日" prop="birthday">
          <el-date-picker
            v-model="formData.birthday"
            type="date"
            value-format="yyyy-MM-dd"
            placeholder="选择生日"
          />
        </el-form-item>

          <el-form-item label="性别" prop="sex">
            <el-select v-model="formData.sex">
              <el-option label='男' value='M' />
              <el-option label='女' value='F' />
            </el-select>
          </el-form-item>
  
          <!-- 联系信息 -->
          <el-form-item label="电话" prop="phone">
            <el-input v-model.trim="formData.phone" />
          </el-form-item>
  
          <el-form-item label="邮箱" prop="email">
            <el-input v-model.trim="formData.email" />
          </el-form-item>
  
          <el-form-item label="地址" prop="addr">
            <el-input v-model.trim="formData.addr" />
          </el-form-item>

          <el-form-item label="学历" prop="education">
            <el-select v-model="formData.education">
              <el-option label="博士" value="PHD" />
              <el-option label="硕士" value="MASTER" />
              <el-option label="本科" value="BACHELOR" />
              <el-option label="大专" value="ASSOCIATE" />
              <el-option label="高中" value="HIGHSCHOOL" />
              <el-option label="其他" value="OTHER" />
            </el-select>
          </el-form-item>

          <el-form-item>
            <el-button type="primary" @click="handleSubmit">提交修改</el-button>
            <el-button @click="resetForm">重置</el-button>
          </el-form-item>
        </el-form>
      </el-card>
    </div>
  </template>
  
  <script setup>
import { ref, reactive } from 'vue'
import { ElMessage } from 'element-plus'
import request from '../utils/request'

/// 使用函数返回初始数据，避免引用问题
const getInitialData = () => ({
  id: null,
  name: '',
  passwd: '',
  birthday: '',
  sex: '',
  phone: '',
  email: '',
  addr: '',
  education: ''
})

// 响应式数据
const editId = ref('')
const loading = ref(false)
const editForm = ref(null)
const formData = reactive(getInitialData())

// 查询用户信息（修复版）
const handleQuery = async () => {
  if (!editId.value) {
    ElMessage.warning('请输入用户ID')
    return
  }

  try {
    loading.value = true
    const response = await request.get('/api/GetUser', {
      params: { 
        page: 1, 
        pagesize: 1, 
        id: editId.value 
      }
    })

    if (response.data?.code === 200 && response.data.data?.length > 0) {
      // 使用深拷贝更新数据
      Object.assign(formData, JSON.parse(JSON.stringify(response.data.data[0])))
    } else {
      ElMessage.warning('用户不存在')
      resetForm(true) // 强制清空
    }
  } catch (error) {
    handleError(error)
    resetForm(true) // 强制清空
  } finally {
    loading.value = false
  }
}

// 提交修改
const handleSubmit = async () => {
  try {
    await editForm.value.validate()
    
    const response = await request.put('/api/EdiUser', formData)
    
    if (response.data?.code === 200) {
      ElMessage.success('修改成功')
      resetForm()
    } else {
      ElMessage.error(response.data?.message || '修改失败')
    }
  } catch (error) {
    if (error.name !== 'ValidationError') {
      handleError(error)
    }
  }
}

// 错误处理
const handleError = (error) => {
  let message = '操作失败'
  if (error.response) {
    switch (error.response.status) {
      case 400:
        message = error.response.data?.message || '参数错误'
        break
      case 404:
        message = '用户不存在'
        break
      case 500:
        message = '服务器内部错误'
        break
    }
  }
  ElMessage.error(message)
}

// 增强版重置方法
const resetForm = () => {
  // 1. 清空查询ID
  editId.value = ''
  
  // 2. 重置数据到初始状态
  Object.assign(formData, {
    id: null,
    name: '',
    passwd: '',
    birthday: '',
    sex: '',
    phone: '',
    email: '',
    addr: '',
    education: ''
  })
  
}

</script>
<style scoped>
.edit-search-container {
  display: flex;
  align-items: center;
}

.edit-form-card {
  margin-top: 20px;
}

.el-form {
  max-width: 600px;
  margin: 0 auto;
}

.el-input, .el-select {
  width: 100%;
}
</style>
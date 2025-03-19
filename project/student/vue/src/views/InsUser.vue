<template>
    <div class="insert-container">
      <el-card shadow="hover">
        <template #header>
          <div class="card-header">
            <span>添加新学生</span>
            <el-button type="primary" @click="submitForm">立即添加</el-button>
          </div>
        </template>
  
        <el-form 
          ref="formRef" 
          :model="formData" 
          label-width="100px"
          label-position="right"
        >
          <el-form-item label="姓名" prop="name">
            <el-input v-model="formData.name" placeholder="请输入学生姓名" clearable />
          </el-form-item>
  
          <el-form-item label="密码" prop="passwd">
            <el-input 
              v-model="formData.passwd" 
              type="password" 
              show-password
              placeholder="请输入登录密码"
            />
          </el-form-item>
  
          <el-form-item label="生日" prop="birthday">
            <el-date-picker
              v-model="formData.birthday"
              type="date"
              placeholder="选择出生日期"
              value-format="YYYY-MM-DD"
            />
          </el-form-item>
  
          <el-form-item label="性别" prop="sex">
            <el-radio-group v-model="formData.sex">
              <el-radio label="男" value="M" />
              <el-radio label="女" value="F"/>
            </el-radio-group>
          </el-form-item>
  
          <el-form-item label="手机号" prop="phone">
            <el-input v-model="formData.phone" placeholder="请输入联系电话" />
          </el-form-item>
  
          <el-form-item label="邮箱" prop="email">
            <el-input v-model="formData.email" placeholder="请输入电子邮箱" />
          </el-form-item>
  
          <el-form-item label="地址" prop="addr">
            <el-input 
              v-model="formData.addr" 
              type="textarea" 
              :rows="2" 
              placeholder="请输入详细地址"
            />
          </el-form-item>
  
          <el-form-item label="学历" prop="education">
            <el-select v-model="formData.education" placeholder="请选择学历">
              <el-option label="小学" value="PRIMARY " />
              <el-option label="初中" value="JUNIORHS" />
              <el-option label="高中" value="HIGHSCHOOL" />
              <el-option label="大专" value="ASSOCIATE" />
              <el-option label="本科" value="BACHELOR" />
              <el-option label="硕士" value="MASTER" />
              <el-option label="博士" value="PHD" />
              <el-option label="其他" value="OTHER" />
            </el-select>
          </el-form-item>
        </el-form>
      </el-card>
    </div>
  </template>
  
  <script setup>
  import { ref, reactive } from 'vue'
  import { ElMessage } from 'element-plus'
  import { useRouter } from 'vue-router'
  import request from '../utils/request'
  
  const router = useRouter()
  const formRef = ref()
  const formData = reactive({
    name: '',
    passwd: '',
    birthday: '',
    sex: 'M',
    phone: '',
    email: '',
    addr: '',
    education: ''
  })

  
  const submitForm = async () => {
    try {
      await formRef.value.validate()
      
      const response = await request.post('/api/InsUser', formData)
      
      if (response.data.code === 200) {
        ElMessage.success('添加成功')
        router.push('/GetUser')
      } else {
        ElMessage.error(response.data.message || '添加失败')
      }
    } catch (error) {
        if (error.response) {
          const serverResponse = error.response.data;
          // 兼容 Spring 默认错误结构（如未覆盖的异常）
          const message = serverResponse.message || serverResponse.error || '未知错误';
          ElMessage.error(`服务器错误: ${message}`);
        } else {
          ElMessage.error('网络连接异常');
        }
      }
  }
  </script>
  
  <style scoped>
  .insert-container {
    max-width: 800px;
    margin: 20px auto;
    padding: 0 15px;
  }
  
  .card-header {
    display: flex;
    justify-content: space-between;
    align-items: center;
  }
  
  .el-form-item {
    margin-bottom: 22px;
  }
  
  .el-select, .el-date-editor {
    width: 100%;
  }
  </style>
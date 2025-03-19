<template>
  <div class="page-container">
    <el-row justify="center" align="middle" class="content-wrapper">
      <el-col :xs="24" :md="12" class="text-center">
        <div class="error-icon">
          <el-icon :size="100" color="#F56C6C">
            <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" fill="currentColor">
              <path d="M11.001 0h2v24h-2z"/>
              <path d="M13.001 0h2v24h-2z"/>
              <path d="M23.121 20.121L3.88.88A3 3 0 0 0 .879 3.88l19.242 19.242a3 3 0 0 0 4.242-4.242z"/>
            </svg>
          </el-icon>
        </div>

        <h1 class="error-code">404</h1>
        <h2 class="error-title">页面不存在</h2>
        
        <p class="error-description">
          您访问的页面可能已删除、暂时不可用或地址输入错误
        </p>

        <div class="action-buttons">
          <el-button 
            type="primary" 
            icon="House"
            @click="goHome"
            class="action-btn"
          >
            返回首页
          </el-button>
          <el-button 
            icon="ArrowLeft" 
            @click="goBack"
            class="action-btn"
          >
            返回上一页
          </el-button>
        </div>
      </el-col>
    </el-row>
  </div>
</template>

<script setup>
import { useRouter } from 'vue-router'
import { ElRow, ElCol, ElIcon, ElButton } from 'element-plus'

const router = useRouter()

const goHome = () => router.push('/')
const goBack = () => {
  if (window.history.length > 1) {
    router.go(-1)
  } else {
    router.replace('/')
  }
}
</script>


<style scoped>
.page-container {
  height: 100vh;
  background: linear-gradient(to right, #4facfe 0%, #ffc0cb 70%);
  /* 兼容旧浏览器 */
  background: -webkit-linear-gradient(left, #4facfe 0%, #ffc0cb 70%);
}

.content-wrapper {
  height: 100%;
}

.text-center {
  text-align: center;
  padding: 40px 20px;
}

.error-icon {
  margin-bottom: 30px;
  animation: bounce 2s infinite;
}

.error-code {
  font-size: 80px;
  color: #303133;
  margin: 20px 0;
  font-weight: bold;
}

.error-title {
  font-size: 24px;
  color: #606266;
  margin: 20px 0;
}

.error-description {
  color: #909399;
  font-size: 16px;
  line-height: 1.6;
  margin: 20px auto;
  max-width: 500px;
}

.action-buttons {
  margin-top: 40px;
  display: flex;
  gap: 16px;
  justify-content: center;
  flex-wrap: wrap;
}

.action-btn {
  padding: 10px 24px;
  min-width: 100px;
  
  /* 调整图标间距 */
  --el-button-icon-margin-right: 8px;
  
  /* 文字居中 */
  display: inline-flex;
  align-items: center;
  justify-content: center;
}

@media (max-width: 768px) {
  .action-buttons {
    flex-direction: column;
    width: 100%;
  }
  
  .action-btn {
    width: 100%;
    margin: 4px 0;
  }
  
  .el-col {
    width: 100%;
    padding: 0 16px;
  }
}

@keyframes bounce {
  0%, 20%, 50%, 80%, 100% {
    transform: translateY(0);
  }
  40% {
    transform: translateY(-30px);
  }
  60% {
    transform: translateY(-15px);
  }
}
</style>
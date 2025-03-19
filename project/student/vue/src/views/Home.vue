<template>
    <div class="dashboard-container">
      <!-- 欢迎横幅 -->
      <el-card class="welcome-banner">
        <div class="banner-content">
          <h1 class="welcome-title">欢迎使用学生管理系统</h1>
          <p class="welcome-subtitle">高效管理学生信息，助力教学数字化</p>
        </div>
      </el-card>
  
      <!-- 数据概览 -->
      <el-row :gutter="20" class="data-overview">
        <el-col :xs="24" :sm="12" :md="6" v-for="(item,index) in statsData" :key="index">
          <el-card class="stat-card" shadow="hover">
            <div class="stat-content">
              <el-icon class="stat-icon" :color="item.color">
                <component :is="item.icon" />
              </el-icon>
              <div class="stat-info">
                <div class="stat-value">{{ item.value }}</div>
                <div class="stat-label">{{ item.label }}</div>
              </div>
            </div>
          </el-card>
        </el-col>
      </el-row>
  
      <!-- 快捷操作 -->
      <el-card class="quick-actions">
        <template #header>
          <div class="card-header">
            <span class="header-title">常用功能</span>
          </div>
        </template>
        <el-row :gutter="20">
          <el-col :xs="12" :sm="6" v-for="(action,index) in quickActions" :key="index">
            <el-button 
              class="action-btn" 
              :type="action.type" 
              :icon="action.icon" 
              @click="router.push(action.path)"
            >
              {{ action.label }}
            </el-button>
          </el-col>
        </el-row>
      </el-card>
  
      
    </div>
  </template>
  
  <script setup>
  import { ref } from 'vue'
  import { useRouter } from 'vue-router'
  import {
    User, Delete, Edit, Plus,
    UserFilled, List, Clock, TrendCharts
  } from '@element-plus/icons-vue'
  
  const router = useRouter()
  
  
  // 快捷操作项
  const quickActions = ref([
    {
      label: '查询学生信息',
      icon: User,
      type: 'success',
      path: '/GetUser'
    },
    {
      label: '删除学生信息',
      icon: Delete,
      type: 'primary',
      path: '/DelUser'
    },
    {
      label: '添加学生信息',
      icon: Plus,
      type: 'primary',
      path: '/InsUser'
    },
    {
      label: '修改学生信息',
      icon: Edit,
      type: 'warning',
      path: '/EdiUser'
    },
  ])
  
  
  </script>
  
  <style scoped>
  .dashboard-container {
    max-width: 1200px;
    margin: 0 auto;
    padding: 20px;
  }
  
  .welcome-banner {
    margin-bottom: 20px;
    background: linear-gradient(135deg, #545c64 0%, #2c3e50 100%);
    border: none;
  }
  
  .banner-content {
    padding: 40px 20px;
    text-align: center;
  }
  
  .welcome-title {
    color: #fff;
    font-size: 2.5rem;
    margin-bottom: 15px;
  }
  
  .welcome-subtitle {
    color: rgba(255, 255, 255, 0.9);
    font-size: 1.2rem;
  }
  
  .data-overview {
    margin-bottom: 20px;
  }
  
  .stat-card {
    margin-bottom: 20px;
    transition: transform 0.3s;
  }
  
  .stat-card:hover {
    transform: translateY(-5px);
  }
  
  .stat-content {
    display: flex;
    align-items: center;
    padding: 20px;
  }
  
  .stat-icon {
    font-size: 2.5rem;
    margin-right: 20px;
  }
  
  .stat-value {
    font-size: 1.8rem;
    font-weight: bold;
    color: var(--el-text-color-primary);
  }
  
  .stat-label {
    color: var(--el-text-color-secondary);
    font-size: 0.9rem;
  }
  
  .quick-actions {
    margin-bottom: 20px;
  }
  
  .card-header {
    display: flex;
    justify-content: space-between;
    align-items: center;
  }
  
  .header-title {
    font-size: 1.2rem;
    font-weight: 500;
  }
  
  .action-btn {
    width: 100%;
    height: 80px;
    white-space: normal;
    line-height: 1.4;
    margin-bottom: 10px;
  }
  
  .recent-activities {
    margin-bottom: 20px;
  }
  
  @media (max-width: 768px) {
    .welcome-title {
      font-size: 1.8rem;
    }
  
    .welcome-subtitle {
      font-size: 1rem;
    }
  
    .action-btn {
      height: 60px;
      padding: 10px;
    }
  }
  </style>
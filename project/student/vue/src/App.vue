<template>
  <div id="app">
    <!-- 顶部导航栏 -->
    <el-menu
      :default-active="activeIndex"
      mode="horizontal"
      router
      class="nav-container"
      background-color="#545c64"
      text-color="#fff"
      active-text-color="#ffd04b"
    >
      <el-menu-item index="/" class="brand">
        <img 
          src="./assets/logo.png" 
          alt="Logo"
          class="brand-logo"
        />
        <span class="brand-text">学生管理系统</span>
      </el-menu-item>

      <!-- 导航菜单项 -->
      <el-menu-item index="/GetUser">
        <el-icon><User /></el-icon>
        <template #title>查询学生信息</template>
      </el-menu-item>
      
      <el-menu-item index="/DelUser">
        <el-icon><Delete /></el-icon>
        <template #title>删除学生信息</template>
      </el-menu-item>

      <el-menu-item index="/InsUser">
        <el-icon><Plus /></el-icon>
        <template #title>添加学生信息</template>
      </el-menu-item>

      <el-menu-item index="/EdiUser">
        <el-icon><Edit /></el-icon>
        <template #title>修改学生信息</template>
      </el-menu-item>

    </el-menu>

    <!-- 主要内容区域 -->
    <div class="main-content">
      <router-view v-slot="{ Component }">
        <transition name="fade" mode="out-in">
          <component :is="Component" />
        </transition>
      </router-view>
    </div>
  </div>
</template>

<script setup>
import { ref, watch } from 'vue'
import { useRoute } from 'vue-router'
import { User, Delete, ArrowDown, Edit, Plus } from '@element-plus/icons-vue'

const route = useRoute()
const activeIndex = ref('/')

// 自动更新激活菜单项
watch(() => route.path, (newPath) => {
  activeIndex.value = newPath
})
</script>

<style scoped>
.nav-container {
  display: flex;
  justify-content: space-between;
  padding: 0 20px;
  box-shadow: 0 2px 12px 0 rgba(0, 0, 0, 0.1);
  position: fixed;
  width: 100%;
  top: 0;
  z-index: 1000;
}

.brand {
  display: flex;
  align-items: center;
  padding-right: 40px !important;
}

.brand-logo {
  width: 32px;
  height: 32px;
  margin-right: 8px;
}

.brand-text {
  font-size: 1.2rem;
  font-weight: 600;
  letter-spacing: 1px;
}

.nav-extra {
  display: flex;
  align-items: center;
  margin-left: auto;
}

.user-info {
  display: flex;
  align-items: center;
  color: #fff;
  cursor: pointer;
  padding: 0 15px;
  transition: background-color 0.3s;
}

.user-info:hover {
  background-color: rgba(255, 255, 255, 0.1);
}

.username {
  margin: 0 8px;
  font-size: 0.9rem;
}

.main-content {
  margin-top: 60px;
  padding: 20px;
  min-height: calc(100vh - 100px);
}

/* 路由切换动画 */
.fade-enter-active,
.fade-leave-active {
  transition: opacity 0.3s ease;
}

.fade-enter-from,
.fade-leave-to {
  opacity: 0;
}
</style>
import { createRouter, createWebHistory } from 'vue-router'

import Home from '../views/Home.vue'

const routes = [
  {
    path: '/',      // 访问路径
    name: 'Home',   // 路由名称
    component: Home // 组件
  },
  
  { 
    path: '/:pathMatch(.*)*',
    redirect: '/' 
  }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

export default router
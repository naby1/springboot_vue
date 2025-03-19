import { createRouter, createWebHistory } from 'vue-router'
import Home from '../views/Home.vue'
import notfound from '../views/notfound.vue'
const routes = [
  {
    path: '/',      // 访问路径
    name: 'Home',       // 路由名称
    component: Home  // 关联的组件
  },
  {
    path: '/GetUser',      // 访问路径
    name: 'GetUser',       // 路由名称
    component: () => import('../views/GetUser.vue')  // 关联的组件
  },
  {
    path: '/DelUser',      // 访问路径
    name: 'DelUser',       // 路由名称
    component: () => import('../views/DelUser.vue')  // 关联的组件
  },
  {
    path: '/InsUser',      // 访问路径
    name: 'InsUser',       // 路由名称
    component: () => import('../views/InsUser.vue')  // 关联的组件
  },
  {
    path: '/EdiUser',      // 访问路径
    name: 'EdiUser',       // 路由名称
    component: () => import('../views/EdiUser.vue')  // 关联的组件
  },
  {
    path: '/:pathMatch(.*)*',
    component: () => import('../views/notfound.vue')
  }
]

const router = createRouter({
  history: createWebHistory(), // 使用HTML5历史模式
  routes
})

export default router
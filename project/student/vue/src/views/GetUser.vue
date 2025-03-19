<template>
    <div>
        <!-- 搜索栏 -->
        <el-card class="search-bar" shadow="never">
        <el-form :inline="true" :model="searchForm" class="demo-form-inline">
            <el-form-item label="ID">
            <el-input 
                v-model.number="searchForm.id" 
                placeholder="输入ID" 
                clearable
                @change="handleSearch"
            />
            </el-form-item>

            <el-form-item label="姓名">
            <el-input
                v-model.trim="searchForm.name"
                placeholder="输入姓名"
                clearable
                @change="handleSearch"
            />
            </el-form-item>

            <el-form-item label="电话">
            <el-input
                v-model.trim="searchForm.phone"
                placeholder="输入电话"
                clearable
                @change="handleSearch"
            />
            </el-form-item>

            <el-form-item label="性别">
            <el-select
                v-model="searchForm.sex"
                placeholder="选择性别"
                clearable
                @change="handleSearch"
            >
                <el-option label="男" value="M" />
                <el-option label="女" value="F" />
            </el-select>
            </el-form-item>

            <el-form-item>
            <el-button type="primary" @click="handleSearch">搜索</el-button>
            <el-button @click="resetSearch">重置</el-button>
            </el-form-item>
        </el-form>
        </el-card>
        <el-table
            :data="tableData"
            style="width: 100%"
            stripe
            border>
            <!-- 基础信息 -->
            <el-table-column
                prop="id"
                label="ID"
                width="70"
                align="center"
                sortable>
            </el-table-column>

            <el-table-column
                prop="name"
                label="姓名"
                width="100"
                align="center">
            </el-table-column>

            <el-table-column
                prop="passwd"
                label="密码"
                width="100"
                align="center">
            </el-table-column>

            <!-- 个人信息 -->
            <el-table-column
                prop="birthday"
                label="生日"
                width="130"
                align="center"
                sortable>
            </el-table-column>

            <el-table-column
                prop="sex"
                label="性别"
                width="80"
                align="center"
                sortable
                :formatter="formatSex">
            </el-table-column>

            <el-table-column
                prop="phone"
                label="电话"
                width="160"
                align="center">
            </el-table-column>

            <!-- 联系信息 -->
            <el-table-column
                prop="email"
                label="电子邮箱"
                min-width="220"
                align="center">
            </el-table-column>

            <el-table-column
                prop="addr"
                label="地址"
                min-width="250"
                align="center">
            </el-table-column>

            <!-- 其他信息 -->
            <el-table-column
                prop="education"
                label="学历"
                width="120"
                :formatter="formatEducation">
            </el-table-column>

            <el-table-column
                prop="create_time"
                label="创建时间"
                width="180"
                sortable>
            </el-table-column>

            <el-table-column
                prop="last_time"
                label="最后修改时间"
                width="180"
                sortable>
            </el-table-column>
        </el-table>
        <!-- 分页组件 -->
        <el-pagination
        @size-change="handleSizeChange"
        @current-change="handleCurrentChange"
        :current-page="currentPage"
        :page-sizes="[10, 20, 50, 100]"
        :page-size="pageSize"
        layout="total, sizes, prev, pager, next, jumper"
        :total="total"
        style="margin-top: 20px;"
        />
    </div>
</template>
<!--  -->
<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { useRouter } from 'vue-router'
import { debounce } from 'lodash-es'
import request from '../utils/request'

const router = useRouter()

// 响应式数据
const searchForm = reactive({
  id: null,
  name: '',
  phone: '',
  sex: ''
})

const tableData = ref([])
const loading = ref(false)
const currentPage = ref(1)
const pageSize = ref(20)
const total = ref(0)
// 数据加载方法
const loadData = async () => {
  try {
    loading.value = true
    
    // 处理请求参数
    const params = {
      page: currentPage.value,
      pageSize: pageSize.value,
      ...Object.fromEntries(
        Object.entries(searchForm)
          .filter(([_, v]) => v !== '' && v !== null)
      )
    }

    const response = await request.get('/api/GetUser', { params })
    
    if (response.data?.code === 200) {
      tableData.value = processData(response.data.data)
      total.value = response.data.total
    }
  } catch (error) {
    handleError(error)
  } finally {
    loading.value = false
  }
}
// 防抖函数
const debouncedSearch = debounce(loadData, 60)

// 生命周期钩子
onMounted(() => {
  loadData()
})

// 方法定义
const handleSearch = () => {
  currentPage.value = 1
  debouncedSearch()
}

const resetSearch = () => {
  Object.assign(searchForm, {
    id: null,
    name: '',
    phone: '',
    sex: ''
  })
  handleSearch()
}

const processData = (data) => {
  return data.map(item => ({ ...item }))
}

const handleError = (error) => {
  let message = '数据加载失败'
  if (error.response) {
    const serverResponse = error.response.data
    message = serverResponse.message || serverResponse.error || '未知错误'
  } else if (error.request) {
    message = error.code === 'ECONNABORTED' 
      ? '请求超时，请检查网络' 
      : '网络连接异常'
  }
  ElMessage.error(message)
  console.error('错误详情:', error)
}

const handleSizeChange = (newSize) => {
  pageSize.value = newSize
  currentPage.value = 1
  loadData()
}

const handleCurrentChange = (newPage) => {
  currentPage.value = newPage
  loadData()
}

// 表格格式化方法
const formatSex = (row, column, sexValue) => {
  const sexMap = { M: '男', F: '女' }
  return sexMap[sexValue] || '-'
}

const formatEducation = (row, column, eduValue) => {
  const eduMap = {
    PHD: '博士',
    MASTER: '硕士',
    BACHELOR: '本科',
    ASSOCIATE: '大专',
    HIGHSCHOOL: '高中',
    JUNIORHS: '初中',
    PRIMARY: '小学',
    OTHER: '其他'
  }
  return eduMap[eduValue] || '-'
}


</script>

<style scoped>
.search-bar {
  margin-bottom: 20px;
}

.demo-form-inline {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
}

.el-form-item {
  margin-bottom: 0;
}

.el-input {
  width: 180px;
}

.el-select {
  width: 180px;
}

@media (max-width: 768px) {
  .demo-form-inline {
    flex-direction: column;
  }
  
  .el-form-item {
    width: 100%;
  }

  .el-input, .el-select {
    width: 100%;
  }
}
</style>
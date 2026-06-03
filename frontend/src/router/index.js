import Vue from 'vue'
import VueRouter from 'vue-router'
import { getToken, getUser } from '@/utils/auth'

Vue.use(VueRouter)

const routes = [
  { path: '/login', name: 'Login', component: () => import('@/views/Login.vue'), meta: { noAuth: true } },
  { path: '/register', name: 'Register', component: () => import('@/views/Register.vue'), meta: { noAuth: true } },
  {
    path: '/',
    component: () => import('@/views/Layout.vue'),
    redirect: '/home',
    children: [
      { path: 'home', name: 'Home', component: () => import('@/views/Home.vue') },
      { path: 'book/:id', name: 'BookDetail', component: () => import('@/views/BookDetail.vue') },
      { path: 'publish', name: 'PublishBook', component: () => import('@/views/PublishBook.vue') },
      { path: 'my-books', name: 'MyBooks', component: () => import('@/views/MyBooks.vue') },
      { path: 'my-orders', name: 'MyOrders', component: () => import('@/views/MyOrders.vue') },
      { path: 'my-favorites', name: 'MyFavorites', component: () => import('@/views/MyFavorites.vue') },
      { path: 'my-exchange', name: 'MyExchange', component: () => import('@/views/MyExchange.vue') },
      { path: 'my-wants', name: 'MyWants', component: () => import('@/views/MyWants.vue') },
      { path: 'my-footprint', name: 'MyFootprint', component: () => import('@/views/MyFootprint.vue') },
      { path: 'messages', name: 'MessageCenter', component: () => import('@/views/MessageCenter.vue') },
      { path: 'order/:id', name: 'OrderDetail', component: () => import('@/views/OrderDetail.vue') },
      { path: 'chat', name: 'ChatRoom', component: () => import('@/views/ChatRoom.vue') },
      { path: 'profile', name: 'Profile', component: () => import('@/views/Profile.vue') },
      { path: 'pay/result', name: 'PayResult', component: () => import('@/views/PayResult.vue') }
    ]
  },
  {
    path: '/admin',
    component: () => import('@/views/admin/Layout.vue'),
    redirect: '/admin/dashboard',
    meta: { requireAdmin: true },
    children: [
      { path: 'dashboard', name: 'AdminDashboard', component: () => import('@/views/admin/Dashboard.vue') },
      { path: 'users', name: 'AdminUsers', component: () => import('@/views/admin/UserManage.vue') },
      { path: 'books', name: 'AdminBooks', component: () => import('@/views/admin/BookManage.vue') },
      { path: 'orders', name: 'AdminOrders', component: () => import('@/views/admin/OrderManage.vue') },
      { path: 'disputes', name: 'AdminDisputes', component: () => import('@/views/admin/DisputeManage.vue') },
      { path: 'comments', name: 'AdminComments', component: () => import('@/views/admin/CommentManage.vue') },
      { path: 'messages', name: 'AdminMessages', component: () => import('@/views/admin/MessageManage.vue') },
      { path: 'categories', name: 'AdminCategories', component: () => import('@/views/admin/CategoryManage.vue') },
      { path: 'banners', name: 'AdminBanners', component: () => import('@/views/admin/BannerManage.vue') },
      { path: 'logs', name: 'AdminLogs', component: () => import('@/views/admin/LogManage.vue') }
    ]
  },
  { path: '*', name: 'NotFound', component: () => import('@/views/NotFound.vue'), meta: { noAuth: true } }
]

const router = new VueRouter({ routes })

router.beforeEach((to, from, next) => {
  if (to.meta.noAuth) {
    next()
    return
  }
  const token = getToken()
  if (!token) {
    next('/login')
    return
  }
  if (to.meta.requireAdmin) {
    const user = getUser()
    if (!user || user.role !== 'ADMIN') {
      next('/home')
      return
    }
  }
  next()
})

export default router

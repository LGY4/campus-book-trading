<template>
  <div class="login-container">
    <canvas ref="particleCanvas" class="particle-canvas"></canvas>
    <div class="login-content">
      <div class="login-left">
        <h1 class="brand-title">校园二手书流转交易平台</h1>
        <p class="brand-desc">大学生自己的二手书流转交易平台</p>
        <div class="brand-features">
          <div class="feature-item">
            <i class="el-icon-coin"></i>
            <span>便宜</span>
          </div>
          <div class="feature-item">
            <i class="el-icon-lightning"></i>
            <span>便捷</span>
          </div>
          <div class="feature-item">
            <i class="el-icon-s-check"></i>
            <span>保障</span>
          </div>
        </div>
      </div>
      <div class="login-right">
        <div class="login-card">
          <h2 class="login-title">欢迎登录</h2>
          <p class="login-subtitle">登录你的账号</p>
          <el-form ref="loginForm" :model="form" :rules="rules" class="login-form">
            <el-form-item prop="username">
              <el-input
                v-model="form.username"
                prefix-icon="el-icon-user"
                placeholder="请输入用户名"
                size="large"
              />
            </el-form-item>
            <el-form-item prop="password">
              <el-input
                v-model="form.password"
                prefix-icon="el-icon-lock"
                type="password"
                placeholder="请输入密码"
                size="large"
                show-password
                @keyup.enter.native="handleLogin"
              />
            </el-form-item>
            <el-form-item>
              <div class="login-options">
                <el-checkbox v-model="form.remember">记住我</el-checkbox>
              </div>
            </el-form-item>
            <el-form-item>
              <el-button
                type="primary"
                size="large"
                :loading="loading"
                class="login-btn"
                @click="handleLogin"
              >
                登 录
              </el-button>
            </el-form-item>
          </el-form>
          <div class="login-footer">
            还没有账号？
            <router-link to="/register" class="register-link">立即注册</router-link>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script>
import { login } from '@/api/auth'

export default {
  name: 'Login',
  data() {
    return {
      form: {
        username: '',
        password: '',
        remember: false
      },
      rules: {
        username: [{ required: true, message: '请输入用户名', trigger: 'blur' }],
        password: [{ required: true, message: '请输入密码', trigger: 'blur' }]
      },
      loading: false,
      particles: [],
      mouse: { x: null, y: null },
      animationId: null
    }
  },
  mounted() {
    this.initParticles()
    window.addEventListener('resize', this.handleResize)
  },
  beforeDestroy() {
    cancelAnimationFrame(this.animationId)
    window.removeEventListener('resize', this.handleResize)
    window.removeEventListener('mousemove', this.handleMouseMove)
  },
  methods: {
    initParticles() {
      const canvas = this.$refs.particleCanvas
      if (!canvas) return
      const ctx = canvas.getContext('2d')

      const resize = () => {
        canvas.width = window.innerWidth
        canvas.height = window.innerHeight
      }
      resize()
      this.handleResize = resize

      const particleCount = 120
      this.particles = []

      class Particle {
        constructor() {
          this.reset()
        }
        reset() {
          this.x = Math.random() * canvas.width
          this.y = Math.random() * canvas.height
          this.size = Math.random() * 3 + 1
          this.speedX = (Math.random() - 0.5) * 0.5
          this.speedY = Math.random() * 0.5 + 0.2
          this.opacity = Math.random() * 0.5 + 0.3
        }
        update(mouse) {
          this.x += this.speedX
          this.y += this.speedY

          if (mouse.x !== null && mouse.y !== null) {
            const dx = mouse.x - this.x
            const dy = mouse.y - this.y
            const dist = Math.sqrt(dx * dx + dy * dy)
            if (dist < 180) {
              const force = (180 - dist) / 180
              this.speedX += (dx / dist) * force * 0.03
              this.speedY += (dy / dist) * force * 0.03
            }
          }

          this.speedX *= 0.99
          this.speedY *= 0.99
          this.speedY = Math.max(this.speedY, 0.1)

          if (this.y > canvas.height || this.x < -10 || this.x > canvas.width + 10) {
            this.reset()
            this.y = -10
          }
        }
        draw(ctx) {
          ctx.beginPath()
          ctx.arc(this.x, this.y, this.size, 0, Math.PI * 2)
          ctx.fillStyle = `rgba(255, 255, 255, ${this.opacity})`
          ctx.fill()
        }
      }

      for (let i = 0; i < particleCount; i++) {
        this.particles.push(new Particle())
      }

      const handleMouseMove = (e) => {
        this.mouse.x = e.clientX
        this.mouse.y = e.clientY
      }
      window.addEventListener('mousemove', handleMouseMove)
      this.handleMouseMove = handleMouseMove

      const animate = () => {
        ctx.clearRect(0, 0, canvas.width, canvas.height)
        this.particles.forEach(p => {
          p.update(this.mouse)
          p.draw(ctx)
        })

        for (let i = 0; i < this.particles.length; i++) {
          for (let j = i + 1; j < this.particles.length; j++) {
            const dx = this.particles[i].x - this.particles[j].x
            const dy = this.particles[i].y - this.particles[j].y
            const dist = Math.sqrt(dx * dx + dy * dy)
            if (dist < 120) {
              ctx.beginPath()
              ctx.strokeStyle = `rgba(255, 255, 255, ${0.4 * (1 - dist / 120)})`
              ctx.lineWidth = 1
              ctx.moveTo(this.particles[i].x, this.particles[i].y)
              ctx.lineTo(this.particles[j].x, this.particles[j].y)
              ctx.stroke()
            }
          }
        }

        if (this.mouse.x !== null && this.mouse.y !== null) {
          for (const p of this.particles) {
            const dx = this.mouse.x - p.x
            const dy = this.mouse.y - p.y
            const dist = Math.sqrt(dx * dx + dy * dy)
            if (dist < 180) {
              ctx.beginPath()
              ctx.strokeStyle = `rgba(255, 255, 255, ${0.6 * (1 - dist / 180)})`
              ctx.lineWidth = 1.2
              ctx.moveTo(this.mouse.x, this.mouse.y)
              ctx.lineTo(p.x, p.y)
              ctx.stroke()
            }
          }
        }

        this.animationId = requestAnimationFrame(animate)
      }
      animate()
    },
    handleLogin() {
      this.$refs.loginForm.validate(async (valid) => {
        if (!valid) return
        this.loading = true
        try {
          const res = await login({
            username: this.form.username,
            password: this.form.password
          })
          await this.$store.dispatch('user/login', {
            token: res.data.token,
            user: res.data.user,
            remember: this.form.remember
          })
          this.$message.success('登录成功')
          this.$router.push('/home')
        } catch (e) {
          // handled by interceptor
        } finally {
          this.loading = false
        }
      })
    }
  }
}
</script>

<style scoped>
.login-container {
  min-height: 100vh;
  width: 100%;
  background: url('~@/assets/login-bg.jpg') no-repeat center center;
  background-size: cover;
  display: flex;
  align-items: center;
  justify-content: center;
  position: relative;
  overflow: hidden;
}

.particle-canvas {
  position: fixed;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  z-index: 1;
  pointer-events: none;
}

.login-content {
  display: flex;
  width: 100%;
  max-width: 1200px;
  min-height: 100vh;
  align-items: center;
  justify-content: space-between;
  padding: 0 60px;
  position: relative;
  z-index: 2;
}

.login-left {
  flex: 1;
  color: #fff;
  text-shadow: 0 2px 10px rgba(0, 0, 0, 0.3);
  padding-right: 60px;
}

.brand-title {
  font-size: 48px;
  font-weight: 700;
  margin-bottom: 16px;
  line-height: 1.2;
}

.brand-desc {
  font-size: 20px;
  margin-bottom: 40px;
  opacity: 0.9;
}

.brand-features {
  display: flex;
  gap: 40px;
}

.feature-item {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 8px;
}

.feature-item i {
  font-size: 36px;
  background: rgba(255, 255, 255, 0.2);
  width: 70px;
  height: 70px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  backdrop-filter: blur(10px);
}

.feature-item span {
  font-size: 18px;
  font-weight: 600;
}

.login-right {
  width: 420px;
  flex-shrink: 0;
}

.login-card {
  width: 100%;
  padding: 40px;
  background: rgba(255, 255, 255, 0.15);
  backdrop-filter: blur(20px);
  border-radius: 20px;
  border: 1px solid rgba(255, 255, 255, 0.3);
  box-shadow: 0 8px 32px rgba(0, 0, 0, 0.2);
}

.login-title {
  text-align: center;
  font-size: 28px;
  color: #fff;
  margin-bottom: 8px;
}

.login-subtitle {
  text-align: center;
  font-size: 14px;
  color: rgba(255, 255, 255, 0.8);
  margin-bottom: 32px;
}

.login-form {
  width: 100%;
}

.login-form >>> .el-input__inner {
  background: rgba(255, 255, 255, 0.1);
  border: 1px solid rgba(255, 255, 255, 0.3);
  color: #fff;
}

.login-form >>> .el-input__inner::placeholder {
  color: rgba(255, 255, 255, 0.6);
}

.login-form >>> .el-input__prefix {
  color: rgba(255, 255, 255, 0.7);
}

.login-form >>> .el-checkbox__label {
  color: rgba(255, 255, 255, 0.9);
}

.login-form >>> .el-checkbox__inner {
  background: rgba(255, 255, 255, 0.1);
  border-color: rgba(255, 255, 255, 0.3);
}

.login-options {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.login-btn {
  width: 100%;
  background: rgba(255, 255, 255, 0.9);
  border: none;
  color: #333;
  font-weight: 600;
}

.login-btn:hover {
  background: #fff;
}

.login-footer {
  text-align: center;
  font-size: 14px;
  color: rgba(255, 255, 255, 0.8);
  margin-top: 16px;
}

.register-link {
  color: #fff;
  text-decoration: none;
  font-weight: 600;
  text-decoration: underline;
}

@media (max-width: 768px) {
  .login-content {
    flex-direction: column;
    padding: 40px 20px;
  }

  .login-left {
    text-align: center;
    padding-right: 0;
    margin-bottom: 40px;
  }

  .brand-title {
    font-size: 32px;
  }

  .brand-features {
    justify-content: center;
  }

  .login-right {
    width: 100%;
    max-width: 400px;
  }
}
</style>

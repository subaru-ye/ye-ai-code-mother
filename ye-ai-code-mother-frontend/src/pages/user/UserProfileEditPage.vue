<template>
  <div id="userProfileEditPage">
    <div class="page-header">
      <h1>编辑个人信息</h1>
      <a-button type="link" @click="goBack">返回</a-button>
    </div>

    <a-card title="基本信息" :loading="loading">
      <a-form
        :model="formState"
        :rules="rules"
        layout="vertical"
        autocomplete="off"
        @finish="handleSubmit"
      >
        <a-form-item label="用户名" name="userName">
          <a-input
            v-model:value="formState.userName"
            placeholder="请输入用户名"
            :maxlength="20"
            show-count
          />
        </a-form-item>

        <a-form-item label="头像" name="userAvatar" extra="支持本地图片文件上传或图片链接">
          <a-space :size="12" align="start" style="width: 100%">
            <a-upload
              v-model:file-list="fileList"
              :before-upload="beforeUpload"
              accept="image/*"
              list-type="picture-card"
              :max-count="1"
            >
              <div>选择图片</div>
            </a-upload>

            <div class="avatar-actions">
              <a-input v-model:value="formState.userAvatar" placeholder="或粘贴头像图片链接" />
              <a-button v-if="formState.userAvatar" @click="clearAvatar">清空头像</a-button>
            </div>
          </a-space>
          <div v-if="formState.userAvatar" class="avatar-preview">
            <a-image
              :src="formState.userAvatar"
              :width="120"
              :height="120"
              style="border-radius: 50%"
              fallback="data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAYAAAAfFcSJAAAADUlEQVR42mNkYPhfDwAChwGA60e6kgAAAABJRU5ErkJggg=="
            />
          </div>
        </a-form-item>

        <a-form-item label="个人简介" name="userProfile">
          <a-textarea
            v-model:value="formState.userProfile"
            placeholder="介绍一下你自己"
            :rows="4"
            :maxlength="200"
            show-count
          />
        </a-form-item>

        <a-form-item>
          <a-space>
            <a-button type="primary" html-type="submit" :loading="submitting">保存</a-button>
            <a-button @click="resetForm">重置</a-button>
          </a-space>
        </a-form-item>
      </a-form>
    </a-card>
  </div>
</template>

<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { message, Upload } from 'ant-design-vue'
import type { UploadFile, UploadProps } from 'ant-design-vue'
import { updateMyUser, uploadAvatar } from '@/api/userController'
import { useLoginUserStore } from '@/stores/loginUser'

const router = useRouter()
const loginUserStore = useLoginUserStore()

const loading = ref(false)
const submitting = ref(false)
const fileList = ref<UploadFile[]>([])

const formState = reactive<API.UserUpdateMyRequest>({
  userName: '',
  userAvatar: '',
  userProfile: '',
})

const rules = {
  userName: [
    { required: true, message: '请输入用户名', trigger: 'blur' },
    { min: 1, max: 20, message: '用户名长度在1-20个字符', trigger: 'blur' },
  ],
  userAvatar: [],
  userProfile: [{ max: 200, message: '简介最多200字', trigger: 'blur' }],
}

const beforeUpload: UploadProps['beforeUpload'] = async (file) => {
  const raw = file as unknown as File
  if (!raw.type?.startsWith('image/')) {
    message.error('请选择图片文件')
    return Upload.LIST_IGNORE
  }
  const maxSizeMb = 2
  if (raw.size > maxSizeMb * 1024 * 1024) {
    message.error(`图片不能超过 ${maxSizeMb}MB`)
    return Upload.LIST_IGNORE
  }
  try {
    const formData = new FormData()
    formData.append('file', raw)
    // 注意：uploadAvatar 在接口文件里默认带了 application/json，这里通过 options 覆盖 headers，
    // 让浏览器/axios 自动为 FormData 设置 multipart/form-data（包含 boundary）
    const res = await uploadAvatar(formData, { headers: {} })
    if (res.data.code !== 0 || !res.data.data) {
      message.error('上传头像失败：' + (res.data.message || '未知错误'))
      return Upload.LIST_IGNORE
    }
    const url = res.data.data
    formState.userAvatar = url
    fileList.value = [
      {
        uid: file.uid,
        name: file.name,
        status: 'done',
        url,
      },
    ]
  } catch (e) {
    console.error('上传头像失败：', e)
    message.error('上传头像失败')
    return Upload.LIST_IGNORE
  }
  // 阻止 a-upload 自带的上传行为（我们已手动上传）
  return false
}

const clearAvatar = () => {
  formState.userAvatar = ''
  fileList.value = []
}

const fillFormFromLoginUser = () => {
  const u = loginUserStore.loginUser
  formState.userName = u.userName || ''
  formState.userAvatar = u.userAvatar || ''
  formState.userProfile = u.userProfile || ''
  fileList.value = formState.userAvatar
    ? [
        {
          uid: '-1',
          name: 'avatar',
          status: 'done',
          url: formState.userAvatar,
        },
      ]
    : []
}

const ensureLogin = async () => {
  if (!loginUserStore.loginUser.id) {
    loading.value = true
    try {
      await loginUserStore.fetchLoginUser()
    } finally {
      loading.value = false
    }
  }
  if (!loginUserStore.loginUser.id) {
    message.warning('请先登录')
    await router.push('/user/login')
    return false
  }
  return true
}

const handleSubmit = async () => {
  if (!(await ensureLogin())) return
  submitting.value = true
  try {
    const res = await updateMyUser({
      userName: formState.userName,
      userAvatar: formState.userAvatar,
      userProfile: formState.userProfile,
    })
    if (res.data.code === 0) {
      message.success('保存成功')
      await loginUserStore.fetchLoginUser()
      fillFormFromLoginUser()
    } else {
      message.error('保存失败：' + res.data.message)
    }
  } catch (e) {
    console.error('保存失败：', e)
    message.error('保存失败')
  } finally {
    submitting.value = false
  }
}

const resetForm = () => {
  fillFormFromLoginUser()
}

const goBack = () => {
  router.back()
}

onMounted(async () => {
  if (!(await ensureLogin())) return
  fillFormFromLoginUser()
})
</script>

<style scoped>
#userProfileEditPage {
  padding: 24px;
  max-width: 900px;
  margin: 0 auto;
}

.page-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 16px;
}

.page-header h1 {
  margin: 0;
  font-size: 24px;
  font-weight: 600;
}

.avatar-preview {
  margin-top: 12px;
}

.avatar-actions {
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 8px;
  min-width: 0;
}
</style>


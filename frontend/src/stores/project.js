import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import axios from 'axios'

const normalizeProject = (project = {}) => ({
  ...project,
  projectName: project.projectName ?? project.project_name ?? '',
  projectCode: project.projectCode ?? project.project_code ?? '',
  projectToken: project.projectToken ?? project.project_token ?? '',
  projectType: project.projectType ?? project.project_type ?? '',
  usageMode: project.usageMode ?? project.usage_mode ?? '',
  deviceBindEnabled: project.deviceBindEnabled ?? project.enable_device_bind ?? false,
  bindType: project.bindType ?? project.device_bind_mode ?? '',
  webhookEnabled: project.webhookEnabled ?? project.webhook_enabled ?? false,
  webhookUrl: project.webhookUrl ?? project.webhook_url ?? '',
  webhookSecret: project.webhookSecret ?? project.webhook_secret ?? ''
})

const toApiProject = (project = {}) => ({
  project_name: project.projectName,
  project_code: project.projectCode,
  project_type: project.projectType,
  usage_mode: project.usageMode,
  status: project.status,
  remark: project.description,
  enable_device_bind: project.deviceBindEnabled,
  device_bind_mode: project.bindType,
  webhook_enabled: project.webhookEnabled,
  webhook_url: project.webhookUrl,
  webhook_secret: project.webhookSecret
})

export const useProjectStore = defineStore('project', () => {
  const projects = ref([])
  const currentProject = ref(null)
  const loading = ref(false)

  const currentProjectToken = computed(() => currentProject.value?.projectToken)
  const currentProjectId = computed(() => currentProject.value?.id)

  async function loadProjects() {
    loading.value = true
    try {
      const { data } = await axios.get('/admin/projects')
      projects.value = (data.data || []).map(normalizeProject)

      const lastProjectId = localStorage.getItem('currentProjectId')
      if (lastProjectId) {
        const project = projects.value.find(p => p.id === parseInt(lastProjectId))
        if (project) currentProject.value = project
      }

      if (!currentProject.value && projects.value.length > 0) {
        currentProject.value = projects.value[0]
      }
    } catch (error) {
      console.error('Failed to load projects:', error)
    } finally {
      loading.value = false
    }
  }

  function switchProject(project) {
    currentProject.value = normalizeProject(project)
    localStorage.setItem('currentProjectId', project.id)
  }

  async function createProject(projectData) {
    try {
      const { data } = await axios.post('/admin/projects', toApiProject(projectData))
      if (data.success) {
        await loadProjects()
        return data
      }
      return data
    } catch (error) {
      console.error('Failed to create project:', error)
      throw error
    }
  }

  async function deleteProject(projectId) {
    try {
      const { data } = await axios.delete(`/admin/projects/${projectId}`)
      if (data.success) {
        if (currentProject.value?.id === projectId) {
          currentProject.value = null
          localStorage.removeItem('currentProjectId')
        }
        await loadProjects()
      }
      return data
    } catch (error) {
      console.error('Failed to delete project:', error)
      throw error
    }
  }

  return {
    projects,
    currentProject,
    currentProjectToken,
    currentProjectId,
    loading,
    loadProjects,
    switchProject,
    createProject,
    deleteProject,
    normalizeProject,
    toApiProject
  }
})

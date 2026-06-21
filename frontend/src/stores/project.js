import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import axios from 'axios'

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
      projects.value = data.data || []

      const lastProjectId = localStorage.getItem('currentProjectId')
      if (lastProjectId) {
        const project = projects.value.find(p => p.id === parseInt(lastProjectId))
        if (project) {
          currentProject.value = project
        }
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
    currentProject.value = project
    localStorage.setItem('currentProjectId', project.id)
  }

  async function createProject(projectData) {
    try {
      const { data } = await axios.post('/admin/projects', projectData)
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

  return {
    projects,
    currentProject,
    currentProjectToken,
    currentProjectId,
    loading,
    loadProjects,
    switchProject,
    createProject
  }
})

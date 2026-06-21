<template>
  <router-view />
</template>

<script setup>
// App entry point
</script>

<style>
/* 全局样式重置 */
* {
  margin: 0;
  padding: 0;
  box-sizing: border-box;
}

html, body, #app {
  width: 100%;
  height: 100%;
  overflow: hidden;
}

body {
  font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', 'PingFang SC', 'Hiragino Sans GB',
    'Microsoft YaHei', 'Helvetica Neue', Helvetica, Arial, sans-serif;
  -webkit-font-smoothing: antialiased;
  -moz-osx-font-smoothing: grayscale;
}

/* Element Plus 全局样式覆盖 */
.el-button {
  border-radius: 8px;
}

.el-input__inner, .el-textarea__inner {
  border-radius: 8px;
}

.el-dialog {
  border-radius: 16px;
}

.el-message-box {
  border-radius: 16px;
}

/* 玻璃拟态对话框 */
.glass-dialog .el-dialog {
  background: rgba(255, 255, 255, 0.95);
  backdrop-filter: blur(20px);
}

/* 滚动条样式 */
::-webkit-scrollbar {
  width: 8px;
  height: 8px;
}

::-webkit-scrollbar-track {
  background: rgba(0, 0, 0, 0.1);
  border-radius: 4px;
}

::-webkit-scrollbar-thumb {
  background: rgba(0, 0, 0, 0.2);
  border-radius: 4px;

  &:hover {
    background: rgba(0, 0, 0, 0.3);
  }
}
</style>

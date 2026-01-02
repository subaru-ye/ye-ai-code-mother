<script setup lang="ts">
import { RouterView } from 'vue-router'
</script>

<template>
  <header>
  </header>

  <RouterView />
</template>

<style scoped>
header {
  line-height: 1.5;
  max-height: 100vh;
}

nav {
  width: 100%;
  font-size: 12px;
  text-align: center;
  margin-top: 2rem;
}


nav a:first-of-type {
  border: 0;
}

</style>

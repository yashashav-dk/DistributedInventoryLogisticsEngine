<template>
  <div class="app">
    <header>
      <h1>Distributed Inventory Logistics Engine</h1>
      <p class="subtitle">Real-time stock management with optimistic concurrency control</p>
    </header>
    <Dashboard />
  </div>
</template>

<script setup>
import Dashboard from './components/Dashboard.vue'
</script>

<style>
*,
*::before,
*::after {
  margin: 0;
  padding: 0;
  box-sizing: border-box;
}

body {
  font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif;
  background: #0f1117;
  color: #e1e4e8;
  min-height: 100vh;
}

.app {
  max-width: 1200px;
  margin: 0 auto;
  padding: 2rem;
}

header {
  text-align: center;
  margin-bottom: 2.5rem;
  padding-bottom: 1.5rem;
  border-bottom: 1px solid #21262d;
}

header h1 {
  font-size: 1.75rem;
  font-weight: 600;
  color: #f0f6fc;
}

.subtitle {
  margin-top: 0.5rem;
  color: #8b949e;
  font-size: 0.95rem;
}
</style>

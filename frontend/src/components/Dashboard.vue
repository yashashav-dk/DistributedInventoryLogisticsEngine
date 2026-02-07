<template>
  <div class="dashboard">
    <!-- Controls -->
    <section class="controls">
      <button class="btn btn-primary" @click="fetchInventory" :disabled="loading">
        {{ loading ? 'Loading...' : 'Refresh Stock' }}
      </button>

      <div class="load-test">
        <label>
          SKU:
          <select v-model="testSku">
            <option v-for="item in inventory" :key="item.sku" :value="item.sku">
              {{ item.sku }}
            </option>
          </select>
        </label>

        <label>
          Concurrent Requests:
          <input type="number" v-model.number="concurrentRequests" min="2" max="50" />
        </label>

        <button class="btn btn-danger" @click="simulateLoad" :disabled="simulating">
          {{ simulating ? 'Running...' : 'Simulate High Load' }}
        </button>
      </div>
    </section>

    <!-- Concurrency Test Results -->
    <section v-if="testResult" class="test-results">
      <h2>Concurrency Test Results</h2>
      <div class="result-grid">
        <div class="result-card">
          <span class="result-value">{{ testResult.totalRequests }}</span>
          <span class="result-label">Total Requests</span>
        </div>
        <div class="result-card success">
          <span class="result-value">{{ testResult.successCount }}</span>
          <span class="result-label">Successful</span>
        </div>
        <div class="result-card conflict">
          <span class="result-value">{{ testResult.conflictCount }}</span>
          <span class="result-label">Conflicts (Caught)</span>
        </div>
        <div class="result-card">
          <span class="result-value">{{ testResult.durationMs }}ms</span>
          <span class="result-label">Duration</span>
        </div>
        <div class="result-card">
          <span class="result-value">{{ testResult.finalQuantity }}</span>
          <span class="result-label">Final Quantity</span>
        </div>
        <div class="result-card">
          <span class="result-value">v{{ testResult.finalVersion }}</span>
          <span class="result-label">Entity Version</span>
        </div>
      </div>
      <p class="result-note">
        Conflicts prove optimistic locking is working: concurrent writes to the same row
        are safely rejected instead of silently corrupting data.
      </p>
    </section>

    <!-- Inventory Table -->
    <section class="inventory-table">
      <h2>Current Stock Levels</h2>
      <table>
        <thead>
          <tr>
            <th>SKU</th>
            <th>Product</th>
            <th>Warehouse</th>
            <th>Quantity</th>
            <th>Version</th>
            <th>Actions</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="item in inventory" :key="item.id">
            <td class="mono">{{ item.sku }}</td>
            <td>{{ item.productName }}</td>
            <td>
              <span class="badge" :class="warehouseClass(item.warehouseId)">
                {{ item.warehouseId }}
              </span>
            </td>
            <td class="quantity">{{ item.quantity }}</td>
            <td class="mono">v{{ item.version }}</td>
            <td class="actions">
              <button class="btn-sm btn-add" @click="adjustStock(item.sku, 10)">+10</button>
              <button class="btn-sm btn-sub" @click="adjustStock(item.sku, -10)">-10</button>
            </td>
          </tr>
          <tr v-if="inventory.length === 0">
            <td colspan="6" class="empty">No inventory data. Start the backend and seed data.</td>
          </tr>
        </tbody>
      </table>
    </section>

    <!-- Activity Log -->
    <section class="log-section">
      <h2>Recent Activity Log</h2>
      <div class="log-entries">
        <div v-for="log in logs" :key="log.id" class="log-entry">
          <span class="log-time">{{ formatTime(log.timestamp) }}</span>
          <span class="log-action" :class="log.action === 'RESTOCK' ? 'restock' : 'deduct'">
            {{ log.action }}
          </span>
          <span class="mono">{{ log.sku }}</span>
          <span>{{ log.quantityChange > 0 ? '+' : '' }}{{ log.quantityChange }}</span>
          <span class="log-warehouse">{{ log.warehouseId }}</span>
        </div>
        <div v-if="logs.length === 0" class="empty">No activity logs yet.</div>
      </div>
    </section>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'

const API = '/api/inventory'

const inventory = ref([])
const logs = ref([])
const loading = ref(false)
const simulating = ref(false)
const testSku = ref('SKU-001')
const concurrentRequests = ref(20)
const testResult = ref(null)

async function fetchInventory() {
  loading.value = true
  try {
    const [itemsRes, logsRes] = await Promise.all([
      fetch(API),
      fetch(`${API}/logs`),
    ])
    inventory.value = await itemsRes.json()
    logs.value = await logsRes.json()
  } catch (err) {
    console.error('Failed to fetch inventory:', err)
  } finally {
    loading.value = false
  }
}

async function adjustStock(sku, change) {
  try {
    const res = await fetch(`${API}/update-stock`, {
      method: 'PUT',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ sku, quantityChange: change }),
    })
    if (res.status === 409) {
      alert('Conflict detected! Another update was in progress. Try again.')
    }
    await fetchInventory()
  } catch (err) {
    console.error('Stock update failed:', err)
  }
}

async function simulateLoad() {
  simulating.value = true
  testResult.value = null
  try {
    const res = await fetch(
      `${API}/simulate-load?sku=${testSku.value}&requests=${concurrentRequests.value}`,
      { method: 'POST' }
    )
    testResult.value = await res.json()
    await fetchInventory()
  } catch (err) {
    console.error('Simulation failed:', err)
  } finally {
    simulating.value = false
  }
}

function formatTime(ts) {
  return new Date(ts).toLocaleTimeString()
}

function warehouseClass(id) {
  const map = { 'WH-EAST': 'wh-east', 'WH-WEST': 'wh-west', 'WH-NORTH': 'wh-north', 'WH-SOUTH': 'wh-south' }
  return map[id] || ''
}

onMounted(fetchInventory)
</script>

<style scoped>
.dashboard {
  display: flex;
  flex-direction: column;
  gap: 2rem;
}

/* ── Controls ────────────────────────────────── */
.controls {
  display: flex;
  align-items: center;
  gap: 1.5rem;
  flex-wrap: wrap;
}

.load-test {
  display: flex;
  align-items: center;
  gap: 1rem;
  margin-left: auto;
  flex-wrap: wrap;
}

.load-test label {
  font-size: 0.85rem;
  color: #8b949e;
  display: flex;
  align-items: center;
  gap: 0.4rem;
}

.load-test select,
.load-test input {
  background: #161b22;
  border: 1px solid #30363d;
  color: #e1e4e8;
  border-radius: 6px;
  padding: 0.35rem 0.5rem;
  font-size: 0.85rem;
}

.load-test input[type='number'] {
  width: 60px;
}

/* ── Buttons ─────────────────────────────────── */
.btn {
  padding: 0.5rem 1.2rem;
  border: none;
  border-radius: 6px;
  font-size: 0.9rem;
  font-weight: 500;
  cursor: pointer;
  transition: opacity 0.15s;
}

.btn:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

.btn-primary {
  background: #238636;
  color: #fff;
}

.btn-primary:hover:not(:disabled) {
  background: #2ea043;
}

.btn-danger {
  background: #da3633;
  color: #fff;
}

.btn-danger:hover:not(:disabled) {
  background: #f85149;
}

.btn-sm {
  padding: 0.2rem 0.6rem;
  border: 1px solid #30363d;
  border-radius: 4px;
  background: #161b22;
  color: #e1e4e8;
  font-size: 0.8rem;
  cursor: pointer;
}

.btn-add:hover { background: #238636; border-color: #238636; }
.btn-sub:hover { background: #da3633; border-color: #da3633; }

/* ── Test Results ────────────────────────────── */
.test-results {
  background: #161b22;
  border: 1px solid #30363d;
  border-radius: 8px;
  padding: 1.5rem;
}

.test-results h2 {
  font-size: 1.1rem;
  margin-bottom: 1rem;
}

.result-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(140px, 1fr));
  gap: 1rem;
}

.result-card {
  background: #0d1117;
  border: 1px solid #21262d;
  border-radius: 6px;
  padding: 1rem;
  text-align: center;
}

.result-card.success { border-color: #238636; }
.result-card.conflict { border-color: #da3633; }

.result-value {
  display: block;
  font-size: 1.6rem;
  font-weight: 700;
  color: #f0f6fc;
}

.result-label {
  display: block;
  font-size: 0.75rem;
  color: #8b949e;
  margin-top: 0.3rem;
  text-transform: uppercase;
  letter-spacing: 0.5px;
}

.result-note {
  margin-top: 1rem;
  font-size: 0.85rem;
  color: #8b949e;
  border-left: 3px solid #da3633;
  padding-left: 0.75rem;
}

/* ── Table ───────────────────────────────────── */
.inventory-table h2 {
  font-size: 1.1rem;
  margin-bottom: 0.75rem;
}

table {
  width: 100%;
  border-collapse: collapse;
  background: #161b22;
  border-radius: 8px;
  overflow: hidden;
}

th, td {
  padding: 0.75rem 1rem;
  text-align: left;
  border-bottom: 1px solid #21262d;
}

th {
  background: #0d1117;
  font-size: 0.8rem;
  text-transform: uppercase;
  letter-spacing: 0.5px;
  color: #8b949e;
}

.mono {
  font-family: 'SF Mono', 'Fira Code', monospace;
  font-size: 0.85rem;
}

.quantity {
  font-weight: 600;
  font-size: 1.05rem;
}

.actions {
  display: flex;
  gap: 0.4rem;
}

.empty {
  text-align: center;
  color: #484f58;
  padding: 2rem;
}

/* ── Warehouse badges ────────────────────────── */
.badge {
  display: inline-block;
  padding: 0.2rem 0.5rem;
  border-radius: 12px;
  font-size: 0.75rem;
  font-weight: 500;
}

.wh-east  { background: #1f3a5f; color: #58a6ff; }
.wh-west  { background: #3b2f1e; color: #d29922; }
.wh-north { background: #1a3a2a; color: #3fb950; }
.wh-south { background: #3d1e28; color: #f778ba; }

/* ── Activity Log ────────────────────────────── */
.log-section h2 {
  font-size: 1.1rem;
  margin-bottom: 0.75rem;
}

.log-entries {
  background: #161b22;
  border: 1px solid #30363d;
  border-radius: 8px;
  max-height: 300px;
  overflow-y: auto;
}

.log-entry {
  display: flex;
  align-items: center;
  gap: 1rem;
  padding: 0.6rem 1rem;
  border-bottom: 1px solid #21262d;
  font-size: 0.85rem;
}

.log-entry:last-child {
  border-bottom: none;
}

.log-time {
  color: #484f58;
  min-width: 80px;
}

.log-action {
  padding: 0.15rem 0.4rem;
  border-radius: 4px;
  font-size: 0.75rem;
  font-weight: 600;
  min-width: 65px;
  text-align: center;
}

.log-action.restock { background: #1a3a2a; color: #3fb950; }
.log-action.deduct  { background: #3d1e28; color: #f85149; }

.log-warehouse {
  margin-left: auto;
  color: #8b949e;
}
</style>

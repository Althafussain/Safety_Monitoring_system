const initialState = {
  threatLevel: 24,
  monitoring: false,
  panicActive: false,
  panicCountdown: 0,
  statusMessage: 'The system is standing by and monitoring your surroundings.',
  contacts: [
    { id: 1, name: 'Maya', phone: '+1 555-0101', role: 'Primary contact' },
    { id: 2, name: 'Alex', phone: '+1 555-0102', role: 'Family' }
  ],
  incidents: [
    { id: 1, title: 'Low-risk movement detected', time: '10 mins ago', level: 'Low' },
    { id: 2, title: 'Emergency alert sent', time: '1 hour ago', level: 'High' }
  ],
  settings: {
    sms: true,
    push: true,
    threshold: 70
  },
  emotion: 'Neutral',
  motion: 'Still',
  objects: 'No objects'
};

let state = JSON.parse(JSON.stringify(initialState));
let panicTimer = null;
let detectionTimer = null;
let cameraStream = null;
let faceApiReady = false;
let isLoadingModels = false;
let cocoModel = null;
const SMOOTHING_WINDOW = 5;
let expressionHistory = [];
let objectHistory = [];
let lastEmergencyAlertAt = 0;

const MODEL_URL = 'https://cdn.jsdelivr.net/gh/justadudewhohacks/face-api.js@0.22.2/weights';
const tabs = document.querySelectorAll('.tab');
const views = document.querySelectorAll('.view');
const quickCards = document.querySelectorAll('.quick-card');

function render() {
  document.getElementById('status-pill').textContent = state.monitoring ? 'Monitoring' : 'Ready';
  document.getElementById('threat-title').textContent = state.panicActive ? 'Emergency alert triggered' : getThreatTitle(state.threatLevel);
  document.getElementById('threat-message').textContent = state.panicActive ? 'A panic sequence is active. Contacts are being notified.' : state.statusMessage;
  document.getElementById('monitoring-status').textContent = state.monitoring ? 'Active' : 'Inactive';
  document.getElementById('contacts-count').textContent = state.contacts.length;
  document.getElementById('incidents-count').textContent = state.incidents.length;
  document.getElementById('threat-score').textContent = `${state.threatLevel}%`;
  document.getElementById('monitor-badge').textContent = state.monitoring ? 'Live' : 'Standby';
  document.getElementById('emotion-pill').textContent = state.emotion;
  document.getElementById('motion-pill').textContent = state.monitoring ? 'Detecting' : 'Still';
  document.getElementById('objects-pill').textContent = state.objects;

  const gauge = document.getElementById('gauge');
  const hue = Math.max(120, 360 - state.threatLevel * 2.8);
  gauge.style.background = `conic-gradient(hsl(${hue} 70% 56%) 0deg, hsl(${hue} 70% 50%) ${state.threatLevel * 3.6}deg, rgba(255,255,255,0.08) ${state.threatLevel * 3.6}deg)`;

  document.getElementById('monitor-toggle').textContent = state.monitoring ? 'Stop monitoring' : 'Start monitoring';
  document.getElementById('panic-btn').textContent = state.panicActive ? `Cancel SOS (${state.panicCountdown}s)` : 'Trigger SOS';
  document.getElementById('sms-toggle').checked = state.settings.sms;
  document.getElementById('push-toggle').checked = state.settings.push;
  document.getElementById('threshold-range').value = state.settings.threshold;
  document.getElementById('threshold-value').textContent = `Threshold: ${state.settings.threshold}%`;

  const cameraShell = document.getElementById('camera-shell');
  const cameraStatus = document.getElementById('camera-status');
  cameraShell.classList.toggle('active', state.monitoring);
  cameraStatus.textContent = state.monitoring
    ? (faceApiReady ? 'Live emotion analysis active' : 'Starting camera and face model...')
    : 'Camera will start when monitoring begins.';

  renderContacts();
  renderHistory();
}

// --- Persistence helpers -----------------------------------------------
function loadState() {
  try {
    const raw = localStorage.getItem('safeguard_state');
    if (raw) {
      const parsed = JSON.parse(raw);
      // merge parsed contacts/incidents/settings into current state
      if (Array.isArray(parsed.contacts)) state.contacts = parsed.contacts.concat(state.contacts.filter(c => !parsed.contacts.some(pc => pc.id === c.id)));
      if (Array.isArray(parsed.incidents)) state.incidents = parsed.incidents.concat(state.incidents.filter(i => !parsed.incidents.some(pi => pi.id === i.id)));
      if (parsed.settings) state.settings = Object.assign({}, state.settings, parsed.settings);
    }
  } catch (e) {
    console.warn('Failed to load saved state', e);
  }
}

function saveState() {
  try {
    const toSave = { contacts: state.contacts, incidents: state.incidents, settings: state.settings };
    localStorage.setItem('safeguard_state', JSON.stringify(toSave));
  } catch (e) {
    console.warn('Failed to save state', e);
  }
}

function triggerEmergencyAlert(emotionName, confidencePercent) {
  const shouldNotify = state.settings.sms || state.settings.push;
  if (!shouldNotify) return;

  const now = Date.now();
  if (lastEmergencyAlertAt && now - lastEmergencyAlertAt < 20000) return;
  lastEmergencyAlertAt = now;

  const contacts = state.contacts.length
    ? state.contacts.map((contact) => contact.name).join(', ')
    : 'your emergency contacts';
  const message = `${emotionName} detected with ${confidencePercent}% confidence. Alerting ${contacts}.`;

  state.threatLevel = Math.min(100, Math.max(85, confidencePercent));
  state.statusMessage = message;
  state.incidents.unshift({
    id: Date.now(),
    title: 'Distress alert sent',
    time: 'Just now',
    level: 'High'
  });
  if (state.incidents.length > 8) {
    state.incidents = state.incidents.slice(0, 8);
  }

  if (typeof window !== 'undefined' && 'Notification' in window) {
    if (Notification.permission === 'granted') {
      new Notification('SafeGuard alert', {
        body: message,
        tag: 'safeguard-distress',
        renotify: true
      });
    } else if (Notification.permission === 'default') {
      Notification.requestPermission().catch(() => {});
    }
  }

  saveState();
  render();
}

// Load persisted state at startup
loadState();

function getThreatTitle(level) {
  if (level < 40) return 'Environment appears safe';
  if (level < 60) return 'Mild distress signals detected';
  if (level < 80) return 'Elevated threat level';
  return 'High threat level';
}

function showView(viewName) {
  tabs.forEach((tab) => tab.classList.toggle('active', tab.dataset.view === viewName));
  views.forEach((view) => view.classList.toggle('active', view.id === `${viewName}-view`));
}

function renderContacts() {
  const container = document.getElementById('contacts-list');
  container.innerHTML = state.contacts.map((c) => `
    <div class="list-item" data-id="${c.id}">
      <div>
        <strong>${c.name}</strong>
        <div class="helper-text">${c.role}</div>
      </div>
      <div style="display:flex;gap:8px;align-items:center;">
        <div class="helper-text" style="margin-right:8px;">${c.phone}</div>
        <button class="secondary-btn small contact-edit" data-id="${c.id}">Edit</button>
        <button class="secondary-btn small contact-delete" data-id="${c.id}">Delete</button>
      </div>
    </div>
  `).join('');

  // wire edit/delete handlers
  container.querySelectorAll('.contact-edit').forEach(btn => {
    btn.addEventListener('click', (e) => {
      const id = Number(btn.dataset.id) || btn.dataset.id;
      startEditContact(id);
    });
  });
  container.querySelectorAll('.contact-delete').forEach(btn => {
    btn.addEventListener('click', (e) => {
      const id = Number(btn.dataset.id) || btn.dataset.id;
      deleteContactFromUI(id);
    });
  });
}

let editingContactId = null;

function startEditContact(id) {
  const contact = state.contacts.find(c => String(c.id) === String(id));
  if (!contact) return;
  editingContactId = contact.id;
  const modal = document.getElementById('add-contact-modal');
  document.getElementById('add-name').value = contact.name;
  document.getElementById('add-phone').value = contact.phone;
  document.getElementById('add-role').value = contact.role || '';
  modal.style.display = 'block';
  modal.setAttribute('aria-hidden', 'false');
  document.getElementById('add-name').focus();
}

function deleteContactFromUI(id) {
  if (!confirm('Delete this contact?')) return;
  state.contacts = state.contacts.filter(c => String(c.id) !== String(id));
  saveState();
  render();
}

function renderHistory() {
  const container = document.getElementById('history-list');
  container.innerHTML = state.incidents.map((item) => `
    <div class="list-item">
      <div>
        <strong>${item.title}</strong>
        <div class="helper-text">${item.time}</div>
      </div>
      <span class="badge">${item.level}</span>
    </div>
  `).join('');
}

async function loadFaceApiModels() {
  if (faceApiReady || isLoadingModels) return;
  if (typeof faceapi === 'undefined') {
    throw new Error('Face recognition library could not be loaded.');
  }

  isLoadingModels = true;
  try {
    // load more accurate models (ssdMobilenetv1 + landmarks + expressions)
    await Promise.all([
      faceapi.nets.ssdMobilenetv1.load(MODEL_URL),
      faceapi.nets.faceLandmark68Net.load(MODEL_URL),
      faceapi.nets.faceExpressionNet.load(MODEL_URL)
    ]);
    faceApiReady = true;
    // also try to load coco-ssd model for object detection (if tfjs loaded)
    if (typeof cocoSsd !== 'undefined' && !cocoModel) {
      try {
        cocoModel = await cocoSsd.load();
      } catch (e) {
        console.warn('Failed to load coco-ssd model:', e);
      }
    }
  } catch (error) {
    faceApiReady = false;
    throw new Error(`Face model download failed: ${error.message}`);
  } finally {
    isLoadingModels = false;
  }
}

function stopDetectionLoop() {
  if (detectionTimer) {
    clearInterval(detectionTimer);
    detectionTimer = null;
  }
}

function stopCameraStream() {
  if (cameraStream) {
    cameraStream.getTracks().forEach((track) => track.stop());
    cameraStream = null;
  }
  const video = document.getElementById('camera-feed');
  if (video) {
    video.srcObject = null;
  }
}

function startDetectionLoop() {
  stopDetectionLoop();
  detectionTimer = window.setInterval(async () => {
    try {
      if (!state.monitoring || !cameraStream || !faceApiReady) return;
      const video = document.getElementById('camera-feed');
      if (!video || video.readyState < 2) return;

      // face detection + expressions using ssdMobilenetv1 for improved accuracy
      const detection = await faceapi
        .detectSingleFace(video, new faceapi.SsdMobilenetv1Options({ minConfidence: 0.45 }))
        .withFaceLandmarks()
        .withFaceExpressions();

      if (detection && detection.expressions) {
        // push expression map into history for smoothing
        expressionHistory.push(detection.expressions);
        if (expressionHistory.length > SMOOTHING_WINDOW) expressionHistory.shift();

        // average scores across history
        const avg = {};
        expressionHistory.forEach(map => {
          Object.entries(map).forEach(([k, v]) => { avg[k] = (avg[k] || 0) + v; });
        });
        Object.keys(avg).forEach(k => { avg[k] = avg[k] / expressionHistory.length; });

        const [bestEmotion, confidence] = Object.entries(avg).sort((a, b) => b[1] - a[1])[0];
        const normalized = bestEmotion.charAt(0).toUpperCase() + bestEmotion.slice(1);
        const confidencePercent = Math.round(confidence * 100);
        state.emotion = normalized;
        state.threatLevel = Math.min(100, Math.max(20, confidencePercent));
        state.statusMessage = `${normalized} detected with ${confidencePercent}% confidence.`;

        const distressEmotions = ['sad', 'fearful', 'angry', 'disgusted'];
        if (distressEmotions.includes(bestEmotion.toLowerCase()) && confidencePercent >= 35) {
          triggerEmergencyAlert(normalized, confidencePercent);
          return;
        }
      } else {
        expressionHistory = [];
        state.emotion = 'No face detected';
        state.statusMessage = 'Please face the camera clearly for emotion analysis.';
      }

      // object detection (coco-ssd) with smoothing
      if (cocoModel) {
        try {
          const objs = await cocoModel.detect(video);
          // build label->score map for this frame
          const frameMap = {};
          objs.filter(o => o.score >= 0.4).slice(0, 5).forEach(o => { frameMap[o.class] = Math.max(frameMap[o.class] || 0, o.score); });
          objectHistory.push(frameMap);
          if (objectHistory.length > SMOOTHING_WINDOW) objectHistory.shift();

          // average across history
          const agg = {};
          objectHistory.forEach(m => Object.entries(m).forEach(([k, v]) => { agg[k] = (agg[k] || 0) + v; }));
          Object.keys(agg).forEach(k => { agg[k] = agg[k] / objectHistory.length; });
          const top = Object.entries(agg).sort((a, b) => b[1] - a[1]).slice(0, 2);
          if (top.length) {
            state.objects = top.map(([label, score]) => `${label} (${Math.round(score * 100)}%)`).join(', ');
          } else {
            state.objects = 'No objects detected';
          }
        } catch (e) {
          console.warn('Object detection error', e);
        }
      }

      render();
    } catch (error) {
      state.emotion = 'Analysis paused';
      state.statusMessage = 'Emotion detection paused. Please allow camera access.';
      render();
    }
  }, 700);
}

async function startMonitoring() {
  if (state.monitoring) return;

  state.monitoring = true;
  state.statusMessage = 'Opening camera and loading face analysis...';
  render();

  try {
    if (!navigator.mediaDevices?.getUserMedia) {
      throw new Error('Camera access is not supported in this browser.');
    }

    const video = document.getElementById('camera-feed');
    const stream = await navigator.mediaDevices.getUserMedia({
      video: { facingMode: 'user', width: { ideal: 1280 }, height: { ideal: 720 } },
      audio: false
    });

    cameraStream = stream;
    video.srcObject = stream;
    await video.play();

    try {
      await loadFaceApiModels();
      state.statusMessage = 'Camera is live. Detecting emotion...';
    } catch (modelError) {
      state.statusMessage = `Camera is live, but emotion models could not load: ${modelError.message}`;
      faceApiReady = false;
    }

    render();
    if (faceApiReady) {
      startDetectionLoop();
    }
  } catch (error) {
    stopCameraStream();
    stopDetectionLoop();
    state.monitoring = false;
    state.statusMessage = `Unable to start monitoring: ${error.message}`;
    render();
  }
}

function stopMonitoring() {
  stopDetectionLoop();
  stopCameraStream();
  state.monitoring = false;
  state.emotion = 'Neutral';
  state.motion = 'Still';
  state.statusMessage = 'Monitoring paused. The dashboard is ready for the next session.';
  render();
}

async function toggleMonitoring() {
  if (state.monitoring) {
    stopMonitoring();
    return;
  }

  await startMonitoring();
}

function startPanic() {
  if (state.panicActive) {
    clearInterval(panicTimer);
    state.panicActive = false;
    state.panicCountdown = 0;
    state.statusMessage = 'SOS sequence cancelled.';
    render();
    return;
  }

  state.panicActive = true;
  state.panicCountdown = 5;
  state.threatLevel = 100;
  state.statusMessage = 'Panic sequence activated. Emergency contacts will be notified.';
  render();

  panicTimer = setInterval(() => {
    state.panicCountdown -= 1;
    if (state.panicCountdown <= 0) {
      clearInterval(panicTimer);
      state.panicActive = false;
      state.panicCountdown = 0;
      state.incidents.unshift({ id: Date.now(), title: 'Emergency alert sent', time: 'Just now', level: 'High' });
      state.statusMessage = 'Emergency alert sent to your trusted contacts.';
      render();
    } else {
      render();
    }
  }, 1000);
}

function addContact() {
  // show modal form
  const modal = document.getElementById('add-contact-modal');
  if (!modal) return;
  modal.style.display = 'block';
  modal.setAttribute('aria-hidden', 'false');
  document.getElementById('add-name').focus();
}

function hideAddContactModal() {
  const modal = document.getElementById('add-contact-modal');
  if (!modal) return;
  modal.style.display = 'none';
  modal.setAttribute('aria-hidden', 'true');
  document.getElementById('add-name').value = '';
  document.getElementById('add-phone').value = '';
  document.getElementById('add-role').value = '';
}

function saveAddContactFromModal() {
  const name = document.getElementById('add-name').value.trim();
  const phone = document.getElementById('add-phone').value.trim();
  const role = document.getElementById('add-role').value.trim() || 'Trusted contact';
  if (!name || !phone) {
    alert('Please provide both name and phone number.');
    return;
  }
  if (editingContactId) {
    // update existing
    state.contacts = state.contacts.map(c => String(c.id) === String(editingContactId) ? { ...c, name, phone, role } : c);
    editingContactId = null;
  } else {
    const contact = { id: Date.now(), name, phone, role };
    state.contacts.unshift(contact);
  }
  saveState();
  hideAddContactModal();
  render();
}

function handleSettings() {
  state.settings.sms = document.getElementById('sms-toggle').checked;
  state.settings.push = document.getElementById('push-toggle').checked;
  state.settings.threshold = Number(document.getElementById('threshold-range').value);
  state.statusMessage = `Alert threshold updated to ${state.settings.threshold}%`;
  render();
}

tabs.forEach((tab) => tab.addEventListener('click', () => showView(tab.dataset.view)));
quickCards.forEach((card) => card.addEventListener('click', () => showView(card.dataset.view)));
document.getElementById('monitor-toggle').addEventListener('click', () => {
  toggleMonitoring().catch((error) => {
    state.monitoring = false;
    state.statusMessage = `Unable to start monitoring: ${error.message}`;
    render();
  });
});
document.getElementById('panic-btn').addEventListener('click', startPanic);
document.getElementById('add-contact-btn').addEventListener('click', addContact);
document.getElementById('save-add-contact').addEventListener('click', saveAddContactFromModal);
document.getElementById('cancel-add-contact').addEventListener('click', hideAddContactModal);
document.getElementById('sms-toggle').addEventListener('change', handleSettings);
document.getElementById('push-toggle').addEventListener('change', handleSettings);
document.getElementById('threshold-range').addEventListener('input', handleSettings);

render();

// Enhance modal edit/delete behavior: show delete button when editing and wire handler
(function() {
  const modalId = 'add-contact-modal';
  const delBtnId = 'delete-add-contact';
  function showDeleteInModal(id) {
    const modal = document.getElementById(modalId);
    const delBtn = document.getElementById(delBtnId);
    if (!modal) return;
    try { modal.dataset.editId = String(id); } catch (e) {}
    if (delBtn) delBtn.style.display = 'inline-block';
  }
  function hideDeleteInModal() {
    const modal = document.getElementById(modalId);
    const delBtn = document.getElementById(delBtnId);
    if (delBtn) delBtn.style.display = 'none';
    if (modal && modal.dataset) delete modal.dataset.editId;
    if (typeof editingContactId !== 'undefined') editingContactId = null;
  }

  // wrap existing startEditContact/hideAddContactModal if present
  try {
    if (typeof startEditContact === 'function') {
      const _orig = startEditContact;
      window.startEditContact = function(id) { _orig(id); showDeleteInModal(id); };
    }
    if (typeof hideAddContactModal === 'function') {
      const _origHide = hideAddContactModal;
      window.hideAddContactModal = function() { _origHide(); hideDeleteInModal(); };
    }
  } catch (e) {
    // ignore
  }

  // wire delete button inside modal
  const delBtn = document.getElementById(delBtnId);
  if (delBtn) {
    delBtn.addEventListener('click', () => {
      const modal = document.getElementById(modalId);
      const editId = modal && modal.dataset && modal.dataset.editId ? Number(modal.dataset.editId) : null;
      if (!editId) return;
      if (!confirm('Delete this contact?')) return;
      state.contacts = state.contacts.filter((c) => c.id !== editId);
      saveState();
      hideAddContactModal();
      render();
    });
  }
})();

// Ensure global function bindings are wrapped (some environments bind function names differently)
try {
  if (typeof startEditContact === 'function') {
    const _origStart = startEditContact;
    startEditContact = function(id) {
      _origStart(id);
      const modal = document.getElementById('add-contact-modal');
      const delBtn = document.getElementById('delete-add-contact');
      try { if (modal) modal.dataset.editId = String(id); } catch (e) {}
      if (delBtn) delBtn.style.display = 'inline-block';
    };
    window.startEditContact = startEditContact;
  }
  if (typeof hideAddContactModal === 'function') {
    const _origHide = hideAddContactModal;
    hideAddContactModal = function() {
      _origHide();
      const delBtn = document.getElementById('delete-add-contact');
      if (delBtn) delBtn.style.display = 'none';
      const modal = document.getElementById('add-contact-modal');
      if (modal && modal.dataset) delete modal.dataset.editId;
      if (typeof editingContactId !== 'undefined') editingContactId = null;
    };
    window.hideAddContactModal = hideAddContactModal;
  }
} catch (e) {
  // ignore
}

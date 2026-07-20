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
    <div class="list-item">
      <div>
        <strong>${c.name}</strong>
        <div class="helper-text">${c.role}</div>
      </div>
      <div class="helper-text">${c.phone}</div>
    </div>
  `).join('');
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
    await Promise.all([
      faceapi.nets.tinyFaceDetector.load(MODEL_URL),
      faceapi.nets.faceExpressionNet.load(MODEL_URL)
    ]);
    faceApiReady = true;
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

      const result = await faceapi
        .detectSingleFace(video, new faceapi.TinyFaceDetectorOptions())
        .withFaceExpressions();

      if (result?.expressions) {
        const [bestEmotion, confidence] = Object.entries(result.expressions).sort((a, b) => b[1] - a[1])[0];
        const normalized = bestEmotion.charAt(0).toUpperCase() + bestEmotion.slice(1);
        state.emotion = normalized;
        state.threatLevel = Math.min(100, Math.max(20, Math.round(confidence * 100)));
        state.statusMessage = `${normalized} detected with ${Math.round(confidence * 100)}% confidence.`;
      } else {
        state.emotion = 'No face detected';
        state.statusMessage = 'Please face the camera clearly for emotion analysis.';
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
      video: { facingMode: 'user' },
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
  const name = window.prompt('Contact name');
  const phone = window.prompt('Phone number');
  if (!name || !phone) return;

  state.contacts.unshift({ id: Date.now(), name, phone, role: 'Added from web' });
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
document.getElementById('sms-toggle').addEventListener('change', handleSettings);
document.getElementById('push-toggle').addEventListener('change', handleSettings);
document.getElementById('threshold-range').addEventListener('input', handleSettings);

render();

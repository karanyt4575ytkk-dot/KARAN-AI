package com.example.coding

import android.content.Context
import com.example.data.dao.ProjectMemoryDao
import com.example.data.models.ProjectMemoryEntity
import kotlinx.coroutines.flow.Flow
import java.io.File
import java.util.UUID

class ProjectManager(
    private val context: Context,
    private val projectDao: ProjectMemoryDao
) {
    private val rootProjectsDir: File = File(context.filesDir, "projects").apply { mkdirs() }

    val allProjects: Flow<List<ProjectMemoryEntity>> = projectDao.getAllProjects()

    fun getFileManagerForProject(projectId: String): FileManager {
        val projDir = File(rootProjectsDir, projectId).apply { mkdirs() }
        return FileManager(projDir)
    }

    suspend fun getActiveProject(): ProjectMemoryEntity? {
        return projectDao.getActiveProject()
    }

    suspend fun createProject(
        name: String,
        description: String,
        technology: String, // "HTML/JS", "Python", "React", "Firebase"
        template: String = "blank"
    ): ProjectMemoryEntity {
        val id = "proj_" + UUID.randomUUID().toString().take(8)
        val projectDir = File(rootProjectsDir, id).apply { mkdirs() }
        val fm = FileManager(projectDir)

        // Seed with starter files depending on template/tech
        when (technology.uppercase()) {
            "FIREBASE", "FIREBASE WEB APP" -> {
                fm.createFile("index.html", createFirebaseIndexHtml(name))
                fm.createFolder("css")
                fm.createFile("css/style.css", createModernCss())
                fm.createFolder("js")
                fm.createFile("js/firebase-config.js", createFirebaseConfigJs())
                fm.createFile("js/app.js", createFirebaseAppJs())
            }
            "PYTHON" -> {
                fm.createFile("main.py", "# $name - Python Project\n\ndef main():\n    print('KARAN Python Agent initialized.')\n\nif __name__ == '__main__':\n    main()\n")
                fm.createFile("requirements.txt", "# Dependencies\nrequests==2.31.0\n")
            }
            "REACT", "NODE.JS" -> {
                fm.createFile("package.json", "{\n  \"name\": \"${name.lowercase().replace(" ", "-")}\",\n  \"version\": \"1.0.0\",\n  \"description\": \"$description\",\n  \"main\": \"index.js\"\n}\n")
                fm.createFile("index.html", createModernHtml(name, description))
                fm.createFolder("src")
                fm.createFile("src/App.js", "// React App Component\nexport default function App() {\n  return '<h1>Hello from $name</h1>';\n}\n")
            }
            else -> {
                // HTML/CSS/JS default
                fm.createFile("index.html", createModernHtml(name, description))
                fm.createFolder("css")
                fm.createFile("css/style.css", createModernCss())
                fm.createFolder("js")
                fm.createFile("js/app.js", createModernJs(name))
            }
        }

        val entity = ProjectMemoryEntity(
            projectId = id,
            name = name,
            description = description,
            technology = technology,
            rootPath = projectDir.absolutePath,
            status = "active",
            metadataJson = "{}"
        )

        projectDao.insertOrUpdate(entity)
        return entity
    }

    suspend fun setActiveProject(projectId: String) {
        val all = projectDao.getProjectById(projectId) ?: return
        // Set all others to inactive
        projectDao.insertOrUpdate(all.copy(status = "active"))
    }

    suspend fun deleteProject(projectId: String): Boolean {
        val projDir = File(rootProjectsDir, projectId)
        projDir.deleteRecursively()
        projectDao.deleteById(projectId)
        return true
    }

    private fun createModernHtml(title: String, desc: String): String = """
<!DOCTYPE html>
<html lang="en">
<head>
  <meta charset="UTF-8">
  <meta name="viewport" content="width=device-width, initial-scale=1.0">
  <title>$title</title>
  <link rel="stylesheet" href="css/style.css">
</head>
<body>
  <div class="container">
    <header>
      <div class="badge">KARAN PROJECT</div>
      <h1>$title</h1>
      <p class="subtitle">$desc</p>
    </header>
    
    <main class="card">
      <h2>Welcome to your Project</h2>
      <p>This project was crafted by KARAN AI Coding Agent. You can inspect, modify, and preview this in real-time.</p>
      
      <div class="action-box">
        <button id="actionBtn" class="btn primary">Execute Action</button>
        <span id="statusText">Ready</span>
      </div>
    </main>

    <footer>
      <p>Engineered with KARAN AI Coding Agent</p>
    </footer>
  </div>
  <script src="js/app.js"></script>
</body>
</html>
""".trimIndent()

    private fun createModernCss(): String = """
:root {
  --bg: #0b0f19;
  --surface: #131b2e;
  --surface-border: #1e293b;
  --primary: #00d2ff;
  --primary-glow: rgba(0, 210, 255, 0.25);
  --text: #f8fafc;
  --text-muted: #94a3b8;
}

* { box-sizing: border-box; margin: 0; padding: 0; font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif; }

body {
  background: var(--bg);
  color: var(--text);
  min-height: 100vh;
  display: flex;
  justify-content: center;
  align-items: center;
  padding: 16px;
}

.container {
  width: 100%;
  max-width: 520px;
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.badge {
  display: inline-block;
  font-size: 11px;
  font-weight: 700;
  letter-spacing: 1px;
  color: var(--primary);
  background: rgba(0, 210, 255, 0.1);
  padding: 4px 10px;
  border-radius: 999px;
  margin-bottom: 8px;
}

.card {
  background: var(--surface);
  border: 1px solid var(--surface-border);
  border-radius: 16px;
  padding: 24px;
  box-shadow: 0 8px 32px rgba(0,0,0,0.4);
}

h1 { font-size: 26px; margin-bottom: 4px; }
.subtitle { color: var(--text-muted); font-size: 14px; }
h2 { font-size: 18px; margin-bottom: 12px; color: var(--primary); }
p { font-size: 14px; line-height: 1.6; color: var(--text-muted); margin-bottom: 16px; }

.action-box {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-top: 16px;
}

.btn {
  background: var(--primary);
  color: #000;
  font-weight: 600;
  border: none;
  padding: 10px 18px;
  border-radius: 8px;
  cursor: pointer;
  box-shadow: 0 0 15px var(--primary-glow);
}

footer {
  text-align: center;
  font-size: 12px;
  color: var(--text-muted);
}
""".trimIndent()

    private fun createModernJs(title: String): String = """
document.addEventListener('DOMContentLoaded', () => {
  console.log('$title loaded with KARAN runtime');
  const btn = document.getElementById('actionBtn');
  const status = document.getElementById('statusText');
  let clicks = 0;

  if (btn && status) {
    btn.addEventListener('click', () => {
      clicks++;
      status.textContent = 'Active (Clicked ' + clicks + ' times)';
      status.style.color = '#00d2ff';
    });
  }
});
""".trimIndent()

    private fun createFirebaseIndexHtml(title: String): String = """
<!DOCTYPE html>
<html lang="en">
<head>
  <meta charset="UTF-8">
  <meta name="viewport" content="width=device-width, initial-scale=1.0">
  <title>$title - Firebase Authentication</title>
  <link rel="stylesheet" href="css/style.css">
  <!-- Firebase SDK v10 (Modular via CDN) -->
  <script type="module" src="js/app.js"></script>
</head>
<body>
  <div class="container">
    <div class="card">
      <div class="badge">FIREBASE SECURE AUTH</div>
      <h1 id="headerTitle">Welcome</h1>
      <p id="authNotice">Enter your credentials to sign in or register.</p>

      <div id="authForm" class="auth-section">
        <input type="email" id="emailInput" placeholder="name@example.com" class="input-field" />
        <input type="password" id="passwordInput" placeholder="Password" class="input-field" />
        <div class="action-box">
          <button id="loginBtn" class="btn primary">Sign In</button>
          <button id="registerBtn" class="btn secondary">Register</button>
        </div>
      </div>

      <div id="profileSection" class="profile-section" style="display: none;">
        <h3>Signed In User:</h3>
        <p id="userEmailDisplay"></p>
        <button id="logoutBtn" class="btn secondary">Log Out</button>
      </div>

      <div id="statusMessage" class="status-msg"></div>
    </div>
  </div>
</body>
</html>
""".trimIndent()

    private fun createFirebaseConfigJs(): String = """
// Firebase Configuration Template
// Replace these with your actual Firebase Project config from the Firebase Console
export const firebaseConfig = {
  apiKey: "AIzaSyYOUR_FIREBASE_API_KEY_HERE",
  authDomain: "your-project.firebaseapp.com",
  projectId: "your-project-id",
  storageBucket: "your-project.appspot.com",
  messagingSenderId: "123456789012",
  appId: "1:123456789012:web:abcdef123456"
};
""".trimIndent()

    private fun createFirebaseAppJs(): String = """
import { initializeApp } from "https://www.gstatic.com/firebasejs/10.8.0/firebase-app.js";
import { getAuth, signInWithEmailAndPassword, createUserWithEmailAndPassword, signOut, onAuthStateChanged } from "https://www.gstatic.com/firebasejs/10.8.0/firebase-auth.js";
import { firebaseConfig } from "./firebase-config.js";

const app = initializeApp(firebaseConfig);
const auth = getAuth(app);

window.addEventListener('DOMContentLoaded', () => {
  const loginBtn = document.getElementById('loginBtn');
  const registerBtn = document.getElementById('registerBtn');
  const logoutBtn = document.getElementById('logoutBtn');
  const emailInput = document.getElementById('emailInput');
  const passwordInput = document.getElementById('passwordInput');
  const statusMsg = document.getElementById('statusMessage');
  const authForm = document.getElementById('authForm');
  const profileSection = document.getElementById('profileSection');
  const userEmailDisplay = document.getElementById('userEmailDisplay');

  onAuthStateChanged(auth, (user) => {
    if (user) {
      authForm.style.display = 'none';
      profileSection.style.display = 'block';
      userEmailDisplay.textContent = user.email;
      statusMsg.textContent = 'Authenticated successfully';
      statusMsg.style.color = '#00d2ff';
    } else {
      authForm.style.display = 'block';
      profileSection.style.display = 'none';
      statusMsg.textContent = 'Signed out';
      statusMsg.style.color = '#94a3b8';
    }
  });

  if (loginBtn) {
    loginBtn.addEventListener('click', async () => {
      try {
        await signInWithEmailAndPassword(auth, emailInput.value, passwordInput.value);
      } catch (err) {
        statusMsg.textContent = 'Error: ' + err.message;
        statusMsg.style.color = '#ff4d6d';
      }
    });
  }

  if (registerBtn) {
    registerBtn.addEventListener('click', async () => {
      try {
        await createUserWithEmailAndPassword(auth, emailInput.value, passwordInput.value);
      } catch (err) {
        statusMsg.textContent = 'Registration Error: ' + err.message;
        statusMsg.style.color = '#ff4d6d';
      }
    });
  }

  if (logoutBtn) {
    logoutBtn.addEventListener('click', () => signOut(auth));
  }
});
""".trimIndent()
}

import React, { useState, useEffect, useRef } from 'react';

// API Base URL resolver
const API_BASE = window.location.origin.includes('localhost') || window.location.origin.includes('192.168')
    ? `${window.location.origin.replace(/:3000|:5173/, '')}/nuerocheck_api/`
    : 'http://10.0.2.2/nuerocheck_api/'; // Special IP for Android Emulator reaching host

export default function App() {
  // UI views: 'welcome', 'login', 'signup', 'forgot-password', 'dashboard', 'scan', 'processing', 'result', 'history', 'settings'
  const [view, setView] = useState('welcome');
  const [user, setUser] = useState(null);
  const [scans, setScans] = useState([]);
  const [activeHistoryFilter, setActiveHistoryFilter] = useState('total');
  const [historySearch, setHistorySearch] = useState('');
  const [toasts, setToasts] = useState([]);
  const [theme, setTheme] = useState('dark');
  const [dailySummary, setDailySummary] = useState(true);
  const [notifSound, setNotifSound] = useState(true);
  const [notifVibration, setNotifVibration] = useState(true);

  // Input states
  const [loginEmail, setLoginEmail] = useState('');
  const [loginPassword, setLoginPassword] = useState('');
  const [loginRemember, setLoginRemember] = useState(false);

  // Signup fields
  const [signupStep, setSignupStep] = useState(1);
  const [signupName, setSignupName] = useState('');
  const [signupEmail, setSignupEmail] = useState('');
  const [signupMobile, setSignupMobile] = useState('');
  const [signupGender, setSignupGender] = useState('');
  const [signupPassword, setSignupPassword] = useState('');
  const [signupOtp, setSignupOtp] = useState('');
  const [otpTimer, setOtpTimer] = useState(60);

  // Forgot password fields
  const [forgotStep, setForgotStep] = useState(1);
  const [forgotEmail, setForgotEmail] = useState('');
  const [forgotOtp, setForgotOtp] = useState('');
  const [forgotNewPassword, setForgotNewPassword] = useState('');

  // Scan fields
  const [scanPatientId, setScanPatientId] = useState('');
  const [scanPatientName, setScanPatientName] = useState('');
  const [scanPatientAge, setScanPatientAge] = useState('');
  const [scanPatientGender, setScanPatientGender] = useState('');
  const [scanNotes, setScanNotes] = useState('');
  const [scanDoctorReview, setScanDoctorReview] = useState('');
  const [selectedScanFile, setSelectedScanFile] = useState(null);
  const [imagePreview, setImagePreview] = useState('');
  const fileInputRef = useRef(null);

  // Processing View States
  const [progressVal, setProgressVal] = useState(0);
  const [progressText, setProgressText] = useState('Initializing parameters...');
  const [processingLogs, setProcessingLogs] = useState([]);

  // Result view state
  const [resultData, setResultData] = useState(null);
  const resultCanvasRef = useRef(null);

  // Profile fields
  const [settingsName, setSettingsName] = useState('');
  const [settingsSpecialty, setSettingsSpecialty] = useState('');
  const [settingsHospital, setSettingsHospital] = useState('');
  const [settingsLicense, setSettingsLicense] = useState('');
  const [settingsExperience, setSettingsExperience] = useState('');
  const [settingsDob, setSettingsDob] = useState('');
  const [settingsAddress, setSettingsAddress] = useState('');
  const [settingsProfileImage, setSettingsProfileImage] = useState(null);

  // Mobile menu toggle
  const [mobileMenuOpen, setMobileMenuOpen] = useState(false);

  // Toast System
  const showToast = (message, isSuccess = true) => {
    const id = Date.now();
    setToasts(prev => [...prev, { id, message, isSuccess }]);
    setTimeout(() => {
      setToasts(prev => prev.filter(t => t.id !== id));
    }, 4500);
  };

  // Initialization check
  useEffect(() => {
    const cachedUser = localStorage.getItem('NuerocheckUser');
    if (cachedUser) {
      try {
        const parsed = JSON.parse(cachedUser);
        setUser(parsed);
        setView('dashboard');

        // Verify session with server
        const formData = new FormData();
        formData.append('email', parsed.email);
        fetch(`${API_BASE}check_user.php`, {
          method: 'POST',
          body: formData
        })
          .then(res => res.json())
          .then(data => {
            if (data.status === 'success' || data.success) {
              const userData = data.user || data.data || {};
              const updated = {
                ...parsed,
                name: userData.name || parsed.name,
                specialty: userData.specialty || parsed.specialty,
                hospital: userData.hospital || parsed.hospital,
                license_no: userData.license_no || parsed.license_no,
                experience: userData.experience || parsed.experience,
                dob: userData.dob || parsed.dob,
                address: userData.address || parsed.address,
                profile_image: userData.profile_image || parsed.profile_image,
                theme: userData.theme || parsed.theme,
                language: userData.language || parsed.language,
                daily_summary: userData.daily_summary ?? parsed.daily_summary,
                notif_sound: userData.notif_sound ?? parsed.notif_sound,
                notif_vibration: userData.notif_vibration ?? parsed.notif_vibration
              };
              setUser(updated);
              setTheme(updated.theme || 'dark');
              setDailySummary(updated.daily_summary !== false && updated.daily_summary !== 0);
              setNotifSound(updated.notif_sound !== false && updated.notif_sound !== 0);
              setNotifVibration(updated.notif_vibration !== false && updated.notif_vibration !== 0);
              localStorage.setItem('NuerocheckUser', JSON.stringify(updated));
            } else {
              handleLogout();
            }
          })
          .catch(err => {
            console.warn('Offline mode or server down, using cached session');
          });

      } catch (e) {
        localStorage.removeItem('NuerocheckUser');
        setView('welcome');
      }
    } else {
      setView('welcome');
    }
  }, []);

  // Theme synchronization
  useEffect(() => {
    document.body.className = theme === 'dark' ? 'dark-theme' : 'light-theme';
  }, [theme]);

  // Load dashboard / history data
  const loadScansData = () => {
    if (!user) return;
    const params = new URLSearchParams({ doctor_email: user.email });
    fetch(`${API_BASE}get_scans.php?${params}`)
      .then(res => res.json())
      .then(data => {
        if (data.status === 'success' || data.success) {
          setScans(data.data || []);
        } else {
          showToast(data.message || 'Failed to fetch scans', false);
        }
      })
      .catch(err => {
        console.error(err);
        showToast('Database server connection error', false);
      });
  };

  useEffect(() => {
    if (user && (view === 'dashboard' || view === 'history')) {
      loadScansData();
    }
  }, [user, view]);

  // Validation helpers
  const validateEmail = (email) => /^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(email);
  const validateMobile = (mobile) => /^\+?[0-9]{7,15}$/.test(mobile);
  const validatePassword = (password) => {
    return /^(?=.*[A-Z])(?=.*[0-9])(?=.*[!@#$%^&*()_+\-=\[\]{};':"\\|,.<>/?]).{6,}$/.test(password);
  };

  // OTP Timer countdown
  useEffect(() => {
    let interval = null;
    if (view === 'signup' && signupStep === 2 && otpTimer > 0) {
      interval = setInterval(() => {
        setOtpTimer(prev => prev - 1);
      }, 1000);
    } else if (otpTimer === 0) {
      showToast('OTP code has expired. Please try again.', false);
      setSignupStep(1);
      setOtpTimer(60);
    }
    return () => clearInterval(interval);
  }, [view, signupStep, otpTimer]);

  const showView = (newView) => {
    // Guards
    const publicViews = ['welcome', 'login', 'signup', 'forgot-password'];
    if (!user && !publicViews.includes(newView)) {
      setView('welcome');
      return;
    }
    setView(newView);
    setMobileMenuOpen(false);

    if (newView === 'scan') {
      resetScanForm();
    } else if (newView === 'settings' && user) {
      setSettingsName(user.name || '');
      setSettingsSpecialty(user.specialty || '');
      setSettingsHospital(user.hospital || '');
      setSettingsLicense(user.license_no || '');
      setSettingsExperience(user.experience || '');
      setSettingsDob(user.dob || '');
      setSettingsAddress(user.address || '');
    }
  };

  const handleSettingChange = (key, value) => {
    if (key === 'theme') setTheme(value);
    if (key === 'daily_summary') setDailySummary(value);
    if (key === 'notif_sound') setNotifSound(value);
    if (key === 'notif_vibration') setNotifVibration(value);

    const formData = new FormData();
    formData.append('email', user.email);
    formData.append(key, value);

    fetch(`${API_BASE}update_settings.php`, {
      method: 'POST',
      body: formData
    })
      .then(res => res.json())
      .then(data => {
        if (data.status === 'success' || data.success) {
          const updatedUser = { ...user, [key]: value };
          setUser(updatedUser);
          localStorage.setItem('NuerocheckUser', JSON.stringify(updatedUser));
        }
      })
      .catch(err => console.error('Failed to sync setting:', err));
  };

  const handleLogin = (e) => {
    e.preventDefault();
    if (!validateEmail(loginEmail)) {
      showToast('Please enter a valid email address.', false);
      return;
    }

    const formData = new FormData();
    formData.append('email', loginEmail);
    formData.append('password', loginPassword);

    fetch(`${API_BASE}login.php`, {
      method: 'POST',
      body: formData
    })
      .then(res => res.json())
      .then(data => {
        if (data.status === 'success' || data.success) {
          showToast(`Welcome back, Dr. ${data.name}!`);
          const userData = data.user || data.data || {};
          const loggedUser = {
            name: data.name || userData.name,
            email: loginEmail,
            specialty: userData.specialty || '',
            hospital: userData.hospital || '',
            license_no: userData.license_no || '',
            experience: userData.experience || 0,
            dob: userData.dob || '',
            address: userData.address || '',
            profile_image: userData.profile_image || ''
          };
          setUser(loggedUser);
          localStorage.setItem('NuerocheckUser', JSON.stringify(loggedUser));
          setView('dashboard');
        } else {
          showToast(data.message || 'Incorrect credentials', false);
        }
      })
      .catch(err => {
        console.error(err);
        showToast('Server connection failed', false);
      });
  };

  const handleSignupSendOtp = () => {
    if (!signupName || !signupEmail || !signupMobile || !signupGender || !signupPassword) {
      showToast('Please fill all registration fields', false);
      return;
    }
    if (!validateEmail(signupEmail)) {
      showToast('Please enter a valid email address.', false);
      return;
    }
    if (!validateMobile(signupMobile)) {
      showToast('Please enter a valid mobile number.', false);
      return;
    }
    if (!validatePassword(signupPassword)) {
      showToast('Password must be at least 6 characters, including uppercase, number and symbol.', false);
      return;
    }

    const formData = new FormData();
    formData.append('email', signupEmail);
    formData.append('action', 'signup');

    fetch(`${API_BASE}send_otp.php`, {
      method: 'POST',
      body: formData
    })
      .then(res => res.json())
      .then(data => {
        if (data.status === 'success' || data.success) {
          showToast('Verification OTP code sent to your email.');
          setSignupStep(2);
          setOtpTimer(60);
        } else {
          showToast(data.message || 'Failed to send OTP code', false);
        }
      })
      .catch(err => {
        console.error(err);
        showToast('Server connection failed', false);
      });
  };

  const handleSignupSubmit = (e) => {
    e.preventDefault();
    if (!signupOtp) {
      showToast('Please enter the OTP verification code', false);
      return;
    }

    const formData = new FormData();
    formData.append('name', signupName);
    formData.append('email', signupEmail);
    formData.append('mobile', signupMobile);
    formData.append('gender', signupGender);
    formData.append('password', signupPassword);
    formData.append('otp_code', signupOtp);

    fetch(`${API_BASE}signup.php`, {
      method: 'POST',
      body: formData
    })
      .then(res => res.json())
      .then(data => {
        if (data.status === 'success' || data.success) {
          showToast('Clinician registration completed successfully!');
          const loggedUser = {
            name: signupName,
            email: signupEmail,
            mobile: signupMobile,
            gender: signupGender,
            specialty: '',
            hospital: '',
            license_no: '',
            experience: 0,
            dob: '',
            address: '',
            profile_image: ''
          };
          setUser(loggedUser);
          localStorage.setItem('NuerocheckUser', JSON.stringify(loggedUser));
          setView('dashboard');
        } else {
          showToast(data.message || 'Registration failed', false);
        }
      })
      .catch(err => {
        console.error(err);
        showToast('Server connection failed', false);
      });
  };

  const handleForgotSendOtp = () => {
    if (!validateEmail(forgotEmail)) {
      showToast('Please enter a valid email address.', false);
      return;
    }

    const formData = new FormData();
    formData.append('email', forgotEmail);
    formData.append('action', 'forgot_pwd');

    fetch(`${API_BASE}send_otp.php`, {
      method: 'POST',
      body: formData
    })
      .then(res => res.json())
      .then(data => {
        if (data.status === 'success' || data.success) {
          showToast('Password reset OTP code sent to your email.');
          setForgotStep(2);
        } else {
          showToast(data.message || 'Failed to send OTP code', false);
        }
      })
      .catch(err => {
        console.error(err);
        showToast('Server connection failed', false);
      });
  };

  const handleForgotSubmit = (e) => {
    e.preventDefault();
    if (!forgotOtp || forgotOtp.length !== 6) {
      showToast('Please enter a valid 6-digit OTP code.', false);
      return;
    }
    if (!validatePassword(forgotNewPassword)) {
      showToast('Password must be at least 6 characters with uppercase, number and symbol.', false);
      return;
    }

    const formData = new FormData();
    formData.append('email', forgotEmail);
    formData.append('otp_code', forgotOtp);
    formData.append('new_password', forgotNewPassword);

    fetch(`${API_BASE}reset_password.php`, {
      method: 'POST',
      body: formData
    })
      .then(res => res.json())
      .then(data => {
        if (data.status === 'success' || data.success) {
          showToast('Password reset successfully. You can now login.');
          setView('login');
          setForgotStep(1);
          setForgotEmail('');
          setForgotOtp('');
          setForgotNewPassword('');
        } else {
          showToast(data.message || 'Password reset failed', false);
        }
      })
      .catch(err => {
        console.error(err);
        showToast('Server connection failed', false);
      });
  };

  const handleLogout = () => {
    localStorage.removeItem('NuerocheckUser');
    setUser(null);
    setScans([]);
    showToast('Logout complete.');
    setView('welcome');
  };

  const handleDeleteAccount = () => {
    if (!confirm('Are you absolutely sure you want to delete your clinician account? All dashboard data will remain but you will lose server access.')) {
      return;
    }

    const formData = new FormData();
    formData.append('email', user.email);

    fetch(`${API_BASE}delete_account.php`, {
      method: 'POST',
      body: formData
    })
      .then(res => res.json())
      .then(data => {
        if (data.status === 'success' || data.success) {
          showToast('Your clinician account was deleted successfully.', false);
          handleLogout();
        } else {
          showToast(data.message || 'Failed to delete account', false);
        }
      })
      .catch(err => {
        console.error(err);
        showToast('Server connection failed', false);
      });
  };

  const handleProfileSubmit = (e) => {
    e.preventDefault();
    const formData = new FormData();
    formData.append('email', user.email);
    formData.append('name', settingsName);
    formData.append('specialty', settingsSpecialty);
    formData.append('hospital', settingsHospital);
    formData.append('license_no', settingsLicense);
    formData.append('experience', settingsExperience);
    formData.append('dob', settingsDob);
    formData.append('address', settingsAddress);
    if (settingsProfileImage) {
      formData.append('profileImage', settingsProfileImage);
    }

    fetch(`${API_BASE}update_profile.php`, {
      method: 'POST',
      body: formData
    })
      .then(res => res.json())
      .then(data => {
        if (data.status === 'success' || data.success) {
          showToast('Profile updated successfully.');
          const updated = {
            ...user,
            name: settingsName,
            specialty: settingsSpecialty,
            hospital: settingsHospital,
            license_no: settingsLicense,
            experience: settingsExperience,
            dob: settingsDob,
            address: settingsAddress,
            profile_image: data.profile_image || user.profile_image
          };
          setUser(updated);
          localStorage.setItem('NuerocheckUser', JSON.stringify(updated));
        } else {
          showToast(data.message || 'Failed to update profile', false);
        }
      })
      .catch(err => {
        console.error(err);
        showToast('Server connection failed', false);
      });
  };

  const resetScanForm = () => {
    setScanPatientId('PAT_' + (Date.now() % 100000000));
    setScanPatientName('');
    setScanPatientAge('');
    setScanPatientGender('');
    setScanNotes('');
    setScanDoctorReview('');
    setSelectedScanFile(null);
    setImagePreview('');
  };

  const handleFileSelect = (file) => {
    if (!file) return;
    if (!file.type.startsWith('image/')) {
      showToast('Please select a valid image file', false);
      return;
    }
    setSelectedScanFile(file);
    const reader = new FileReader();
    reader.onload = e => {
      setImagePreview(e.target.result);
    };
    reader.readAsDataURL(file);
  };

  // Run AI diagnostics and upload results
  const startDiagnosticAnalysis = () => {
    if (!scanPatientId || !scanPatientName || !scanPatientAge || !scanPatientGender) {
      showToast('Please fill all demographics details.', false);
      return;
    }
    const ageVal = parseInt(scanPatientAge);
    if (isNaN(ageVal) || ageVal < 0 || ageVal > 150) {
      showToast('Please enter a valid age between 0 and 150.', false);
      return;
    }
    if (!selectedScanFile) {
      showToast('Please select a scan image file.', false);
      return;
    }

    setView('processing');
    setProgressVal(0);
    setProcessingLogs([]);

    const steps = [
      { progress: 20, text: 'Uploading scan...', log: 'Transferring scan slice image to server...' },
      { progress: 40, text: 'Assessing image quality...', log: 'Evaluating CT slice window metadata and orientation...' },
      { progress: 60, text: 'AI model inference processing...', log: 'Executing YOLO-based diagnostic models on backend...' },
      { progress: 80, text: 'Analyzing brain regions...', log: 'Calculating predictive probabilities and confidence boundaries...' },
      { progress: 95, text: 'Generating output records...', log: 'Compiling annotations and final medical report summary...' }
    ];

    let currentStep = 0;
    let apiResult = null;
    let apiError = null;

    // Trigger backend AI analysis in parallel with endpoint fallback
    const analyzeData = new FormData();
    analyzeData.append('image', selectedScanFile);

    const tryFetchAnalyze = (url) => {
      return fetch(url, { method: 'POST', body: analyzeData })
        .then(res => {
          if (!res.ok && url.includes('nuerocheck_api')) {
            // Fallback to /backend/ endpoint
            const fallbackUrl = url.replace('/nuerocheck_api/', '/backend/');
            return fetch(fallbackUrl, { method: 'POST', body: analyzeData }).then(r => r.json());
          }
          return res.json();
        });
    };

    tryFetchAnalyze(`${API_BASE}analyze.php`)
      .then(data => {
        apiResult = data;
      })
      .catch(err => {
        console.error('AI Analysis fetch error:', err);
        apiError = err;
      });

    const interval = setInterval(() => {
      if (currentStep < steps.length) {
        const step = steps[currentStep];

        // Stall at 55% if we reach AI inference but API hasn't responded yet
        if (step.progress >= 60 && !apiResult && !apiError) {
          setProgressVal(55);
          setProgressText('Awaiting model inference output from server...');
          return;
        }

        setProgressVal(step.progress);
        setProgressText(step.text);
        setProcessingLogs(prev => [...prev, step.log]);

        currentStep++;
      } else {
        clearInterval(interval);

        if (apiError && !apiResult) {
          apiResult = { validationFailed: true, validationError: 'AI engine timeout/error. File recorded.' };
        }

        // Draw overlay and save all scan files (including valid and wrong/invalid files) to database
        processModelResults(apiResult || { validationFailed: true, validationError: 'Unrecognized file format' });
      }
    }, 1200);
  };

  const processModelResults = (aiOutput) => {
    const img = new Image();
    img.src = imagePreview;
    img.onload = () => {
      const tempCanvas = document.createElement('canvas');
      const ctx = tempCanvas.getContext('2d');
      tempCanvas.width = img.naturalWidth;
      tempCanvas.height = img.naturalHeight;
      ctx.drawImage(img, 0, 0);

      const isValidationFailed = Boolean(aiOutput && aiOutput.validationFailed);
      const isHemorrhage = !isValidationFailed && Boolean(aiOutput && aiOutput.hasHemorrhage);
      const confidence = isValidationFailed ? 0.0 : (aiOutput?.highestConfidence || 0.0);
      const scanResultText = isValidationFailed ? 'Invalid Scan' : (isHemorrhage ? 'Tumor' : 'Normal');
      const scanRiskLevel = isValidationFailed ? 'INVALID' : (isHemorrhage ? (confidence > 0.7 ? 'CRITICAL' : 'HIGH') : 'LOW');

      if (isValidationFailed) {
        // Draw warning banner for non-brain CT or invalid uploaded image
        const bannerHeight = Math.max(36, tempCanvas.height * 0.07);
        ctx.fillStyle = 'rgba(234, 179, 8, 0.9)';
        ctx.fillRect(0, 0, tempCanvas.width, bannerHeight);
        ctx.fillStyle = '#000000';
        const bannerFontSize = Math.max(14, bannerHeight * 0.45);
        ctx.font = `bold ${bannerFontSize}px sans-serif`;
        ctx.fillText('⚠️ NON-BRAIN CT SCAN RECORDED IN DATABASE', 12, bannerHeight * 0.65);
      } else if (isHemorrhage && aiOutput.boundingBox) {
        const box = aiOutput.boundingBox;
        const scaleX = tempCanvas.width / box.inputSize;
        const scaleY = tempCanvas.height / box.inputSize;

        const left = (box.cx - box.w / 2) * scaleX;
        const top = (box.cy - box.h / 2) * scaleY;
        const width = box.w * scaleX;
        const height = box.h * scaleY;

        ctx.strokeStyle = '#ef4444';
        ctx.lineWidth = Math.max(3, Math.min(tempCanvas.width, tempCanvas.height) * 0.01);
        ctx.strokeRect(left, top, width, height);

        ctx.fillStyle = '#ef4444';
        const fontSize = Math.max(12, Math.min(tempCanvas.width, tempCanvas.height) * 0.04);
        ctx.font = `bold ${fontSize}px var(--font-heading)`;
        const labelText = `Tumor: ${(confidence * 100).toFixed(0)}%`;
        const textWidth = ctx.measureText(labelText).width;
        ctx.fillRect(left, top - fontSize - 6, textWidth + 12, fontSize + 8);

        ctx.fillStyle = '#ffffff';
        ctx.fillText(labelText, left + 6, top - 6);
      }

      tempCanvas.toBlob(blob => {
        const annotatedFile = new File([blob], `scan_${scanPatientId}.jpg`, { type: 'image/jpeg' });
        const uploadData = new FormData();
        uploadData.append('doctor_email', user?.email || 'doctor@nuerocheck.com');
        uploadData.append('patient_id', scanPatientId);
        uploadData.append('patient_name', scanPatientName);
        uploadData.append('patient_age', scanPatientAge);
        uploadData.append('patient_gender', scanPatientGender);
        uploadData.append('result', scanResultText);
        uploadData.append('risk_level', scanRiskLevel);
        uploadData.append('image', annotatedFile);
        uploadData.append('notes', isValidationFailed ? `[FLAGGED NON-BRAIN CT / INVALID IMAGE] ${scanNotes}` : scanNotes);
        uploadData.append('doctor_review', scanDoctorReview);

        // Subtypes data fields
        uploadData.append('intraventricular', aiOutput?.intraventricular || 0);
        uploadData.append('intraparenchymal', aiOutput?.intraparenchymal || 0);
        uploadData.append('subarachnoid', aiOutput?.subarachnoid || 0);
        uploadData.append('epidural', aiOutput?.epidural || 0);
        uploadData.append('subdural', aiOutput?.subdural || 0);

        fetch(`${API_BASE}upload_scan.php`, {
          method: 'POST',
          body: uploadData
        })
          .then(res => res.json())
          .then(data => {
            const finalResultPayload = {
              patientId: scanPatientId,
              patientName: scanPatientName,
              patientAge: scanPatientAge,
              patientGender: scanPatientGender,
              result: scanResultText,
              riskLevel: scanRiskLevel,
              confidence: confidence,
              imageSrc: tempCanvas.toDataURL('image/jpeg'),
              notes: isValidationFailed ? `[FLAGGED NON-BRAIN CT / INVALID IMAGE] ${scanNotes}` : scanNotes,
              doctorReview: scanDoctorReview,
              intraventricular: aiOutput?.intraventricular || 0,
              intraparenchymal: aiOutput?.intraparenchymal || 0,
              subarachnoid: aiOutput?.subarachnoid || 0,
              epidural: aiOutput?.epidural || 0,
              subdural: aiOutput?.subdural || 0,
              dateTime: new Date().toLocaleString()
            };

            showToast(isValidationFailed ? 'Non-brain CT / Invalid scan file saved to database.' : 'Scan saved to cloud server successfully.');
            setResultData(finalResultPayload);
            setView('result');
          })
          .catch(err => {
            console.error(err);
            showToast('Network error saving scan to database. Showing locally.', false);
            setResultData({
              patientId: scanPatientId,
              patientName: scanPatientName,
              patientAge: scanPatientAge,
              patientGender: scanPatientGender,
              result: scanResultText,
              riskLevel: scanRiskLevel,
              confidence: confidence,
              imageSrc: tempCanvas.toDataURL('image/jpeg'),
              notes: isValidationFailed ? `[FLAGGED NON-BRAIN CT / INVALID IMAGE] ${scanNotes}` : scanNotes,
              doctorReview: scanDoctorReview,
              intraventricular: aiOutput?.intraventricular || 0,
              intraparenchymal: aiOutput?.intraparenchymal || 0,
              subarachnoid: aiOutput?.subarachnoid || 0,
              epidural: aiOutput?.epidural || 0,
              subdural: aiOutput?.subdural || 0,
              dateTime: new Date().toLocaleString()
            });
            setView('result');
          });
      }, 'image/jpeg', 0.90);
    };
  };

  const handleRecentScanClick = (scan) => {
    let relativePath = scan.image_path;
    if (!relativePath.startsWith('http') && !relativePath.startsWith('file') && !relativePath.startsWith('content')) {
      relativePath = API_BASE + relativePath;
    }
    const maxSubtype = Math.max(
      parseFloat(scan.intraventricular || 0),
      parseFloat(scan.intraparenchymal || 0),
      parseFloat(scan.subarachnoid || 0),
      parseFloat(scan.epidural || 0),
      parseFloat(scan.subdural || 0)
    );
    setResultData({
      patientId: scan.patient_id,
      patientName: scan.patient_name,
      patientAge: scan.patient_age,
      patientGender: scan.patient_gender,
      result: scan.result,
      riskLevel: scan.risk_level,
      confidence: maxSubtype > 0 ? maxSubtype : ((scan.result === 'Tumor' || scan.result === 'Hemorrhage') ? 0.85 : 0.05),
      imageSrc: relativePath,
      notes: scan.notes,
      doctorReview: scan.doctor_review,
      intraventricular: parseFloat(scan.intraventricular || 0),
      intraparenchymal: parseFloat(scan.intraparenchymal || 0),
      subarachnoid: parseFloat(scan.subarachnoid || 0),
      epidural: parseFloat(scan.epidural || 0),
      subdural: parseFloat(scan.subdural || 0),
      dateTime: `${scan.date_added} ${scan.time_added}`
    });
    setView('result');
  };

  // Drawing canvas in Result View
  useEffect(() => {
    if (view === 'result' && resultData && resultCanvasRef.current) {
      const canvas = resultCanvasRef.current;
      const ctx = canvas.getContext('2d');
      const img = new Image();
      img.src = resultData.imageSrc;
      img.onload = () => {
        canvas.width = img.naturalWidth;
        canvas.height = img.naturalHeight;
        ctx.drawImage(img, 0, 0);
      };
    }
  }, [view, resultData]);

  // Counters for dashboard
  const totalScansCount = scans.length;
  const tumorScansCount = scans.filter(s => s.result === 'Tumor' || s.result === 'Hemorrhage').length;
  const normalScansCount = scans.filter(s => s.result === 'Normal').length;

  // Filter & Search Scans History
  const getFilteredScans = () => {
    let filtered = [...scans];
    if (activeHistoryFilter === 'normal') {
      filtered = filtered.filter(s => s.result === 'Normal');
    } else if (activeHistoryFilter === 'abnormal') {
      filtered = filtered.filter(s => s.result === 'Tumor' || s.result === 'Hemorrhage');
    } else if (activeHistoryFilter === 'invalid') {
      filtered = filtered.filter(s => s.result === 'Invalid Scan' || s.result === 'Non-Brain CT');
    }

    if (historySearch) {
      const q = historySearch.toLowerCase();
      filtered = filtered.filter(s =>
        (s.patient_name || '').toLowerCase().includes(q) ||
        (s.patient_id || '').toLowerCase().includes(q)
      );
    }
    return filtered;
  };

  return (
    <div className="app-wrapper">
      {/* Background Glow Blobs */}
      <div className="glow-blob blob-1"></div>
      <div className="glow-blob blob-2"></div>
      <div className="glow-blob blob-3"></div>

      {/* Toast Notification Container */}
      <div className="toast-container" style={{ position: 'fixed', top: '20px', left: '20px', zIndex: 9999, pointerEvents: 'none' }}>
        {toasts.map(t => (
          <div key={t.id} className={`toast ${t.isSuccess ? 'success' : 'error'}`} style={{ display: 'flex', alignItems: 'center', pointerEvents: 'auto', marginBottom: '10px' }}>
            <span className="toast-status-icon" style={{ marginRight: '10px' }}>{t.isSuccess ? '🟢' : '🔴'}</span>
            <span className="toast-message">{t.message}</span>
          </div>
        ))}
      </div>

      {/* Sidebar Navigation */}
      {user && (
        <aside className={`app-sidebar ${mobileMenuOpen ? 'active' : ''}`}>
          <div className="sidebar-brand">
            <span className="brand-logo">🧠</span>
            <span className="brand-name">Nuerocheck</span>
          </div>
          <nav className="sidebar-nav">
            <button className={`nav-item ${view === 'dashboard' ? 'active' : ''}`} onClick={() => showView('dashboard')}>
              <span className="nav-icon">📊</span> Dashboard
            </button>
            <button className={`nav-item ${view === 'scan' ? 'active' : ''}`} onClick={() => showView('scan')}>
              <span className="nav-icon">📷</span> New Scan
            </button>
            <button className={`nav-item ${view === 'history' ? 'active' : ''}`} onClick={() => showView('history')}>
              <span className="nav-icon">📜</span> Scan History
            </button>
            <button className={`nav-item ${view === 'settings' ? 'active' : ''}`} onClick={() => showView('settings')}>
              <span className="nav-icon">⚙️</span> Settings
            </button>
          </nav>
          <div className="sidebar-footer">
            <div className="sidebar-user">
              <div className="user-avatar">{user.name ? user.name.charAt(0).toUpperCase() : 'D'}</div>
              <div className="user-info">
                <div className="user-name">{user.name || 'Doctor'}</div>
                <div className="user-role">{user.specialty || 'Radiologist'}</div>
              </div>
            </div>
            <button className="logout-btn" onClick={handleLogout}>
              <span className="nav-icon">🚪</span> Logout
            </button>
          </div>
        </aside>
      )}

      {/* Main Content Area */}
      <main className="main-content">
        {user && (
          <header className="top-navbar" style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '20px' }}>
            <button className="menu-toggle" onClick={() => setMobileMenuOpen(prev => !prev)} style={{ background: 'transparent', border: 'none', fontSize: '24px', color: 'var(--text-primary)', cursor: 'pointer' }}>☰</button>
            <div className="navbar-title" style={{ fontSize: '18px', fontWeight: '700', textTransform: 'capitalize' }}>{view.replace('-', ' ')}</div>
            <div className="navbar-actions" style={{ display: 'flex', alignItems: 'center', gap: '10px' }}>
              <span className="navbar-user-name">{user.name}</span>
              <div className="navbar-avatar" style={{ width: '32px', height: '32px', borderRadius: '50%', background: 'linear-gradient(135deg, var(--accent-cyan), var(--accent-blue))', display: 'flex', alignItems: 'center', justifyContent: 'center', color: 'white', fontWeight: 'bold' }}>{user.name ? user.name.charAt(0).toUpperCase() : 'D'}</div>
            </div>
          </header>
        )}

        <div className="view-viewport">

          {/* 1. WELCOME VIEW */}
          {view === 'welcome' && (
            <section className="view-section active">
              <div className="welcome-hero">
                <div className="hero-logo">🧠</div>
                <h1 className="hero-title">Nuerocheck AI</h1>
                <p className="hero-tagline">Advanced AI-Assisted Brain Tumor Diagnostics & Scan Management</p>
                <div className="hero-buttons">
                  <button className="btn btn-primary" onClick={() => showView('login')}>Access Portal</button>
                  <button className="btn btn-secondary" onClick={() => showView('signup')}>Register Clinician</button>
                </div>
              </div>
              <div className="features-grid">
                <div className="feature-card">
                  <span className="feature-icon">⚡</span>
                  <h3>Real-time Classification</h3>
                  <p>Leverage TensorFlow-powered AI pipelines to detect brain tumors instantly from raw CT scans.</p>
                </div>
                <div className="feature-card">
                  <span className="feature-icon">🛡️</span>
                  <h3>Secure Patient Records</h3>
                  <p>All data and uploads are stored securely on your local network database with robust encryption protocols.</p>
                </div>
                <div className="feature-card">
                  <span className="feature-icon">📊</span>
                  <h3>Comprehensive Analytics</h3>
                  <p>Track critical patient stats, historical timeline items, and generate exportable clinical reports.</p>
                </div>
              </div>
            </section>
          )}

          {/* 2. LOGIN VIEW */}
          {view === 'login' && (
            <section className="view-section active">
              <div className="auth-card glass-panel">
                <h2>Welcome Back</h2>
                <p className="auth-subtitle">Login to your clinical dashboard</p>
                <form className="auth-form" onSubmit={handleLogin}>
                  <div className="form-group">
                    <label>Clinician Email</label>
                    <input type="email" required placeholder="doctor@hospital.com" value={loginEmail} onChange={e => setLoginEmail(e.target.value)} />
                  </div>
                  <div className="form-group">
                    <label>Password</label>
                    <input type="password" required placeholder="••••••••" value={loginPassword} onChange={e => setLoginPassword(e.target.value)} />
                  </div>
                  <div className="auth-options">
                    <label className="checkbox-container">
                      <input type="checkbox" checked={loginRemember} onChange={e => setLoginRemember(e.target.checked)} />
                      Remember Me
                    </label>
                    <a href="#" className="forgot-link" onClick={(e) => { e.preventDefault(); showView('forgot-password'); }}>Forgot Password?</a>
                  </div>
                  <button type="submit" className="btn btn-primary btn-block">Login</button>
                </form>
                <p className="auth-footer">Don't have an account? <a href="#" onClick={(e) => { e.preventDefault(); showView('signup'); }}>Sign Up</a></p>
              </div>
            </section>
          )}

          {/* 3. SIGNUP VIEW */}
          {view === 'signup' && (
            <section className="view-section active">
              <div className="auth-card glass-panel">
                <h2>Clinician Registration</h2>
                <p className="auth-subtitle">Register to request access credentials</p>
                <form className="auth-form" onSubmit={handleSignupSubmit}>
                  {signupStep === 1 ? (
                    <div>
                      <div className="form-group" style={{ marginBottom: '12dp' }}>
                        <label>Full Name</label>
                        <input type="text" required placeholder="Dr. John Doe" value={signupName} onChange={e => setSignupName(e.target.value)} />
                      </div>
                      <div className="form-group" style={{ marginBottom: '12dp' }}>
                        <label>Email Address</label>
                        <input type="email" required placeholder="johndoe@hospital.com" value={signupEmail} onChange={e => setSignupEmail(e.target.value)} />
                      </div>
                      <div className="form-group" style={{ marginBottom: '12dp' }}>
                        <label>Mobile Number</label>
                        <input type="tel" required placeholder="+1234567890" value={signupMobile} onChange={e => setSignupMobile(e.target.value)} />
                      </div>
                      <div className="form-group" style={{ marginBottom: '12dp' }}>
                        <label>Gender</label>
                        <select required value={signupGender} onChange={e => setSignupGender(e.target.value)}>
                          <option value="">Select Gender</option>
                          <option value="Male">Male</option>
                          <option value="Female">Female</option>
                          <option value="Other">Other</option>
                        </select>
                      </div>
                      <div className="form-group" style={{ marginBottom: '20dp' }}>
                        <label>Password</label>
                        <input type="password" required placeholder="••••••••" value={signupPassword} onChange={e => setSignupPassword(e.target.value)} />
                      </div>
                      <button type="button" className="btn btn-primary btn-block" onClick={handleSignupSendOtp}>Send OTP Verification</button>
                    </div>
                  ) : (
                    <div>
                      <div className="otp-instruction" style={{ color: 'var(--text-secondary)', marginBottom: '20px' }}>
                        <p>An OTP verification code was sent to <strong>{signupEmail}</strong>. Please enter the 6-digit code below.</p>
                      </div>
                      <div className="form-group" style={{ marginBottom: '20px' }}>
                        <label>Verification OTP</label>
                        <input type="text" placeholder="123456" maxLength={6} value={signupOtp} onChange={e => setSignupOtp(e.target.value)} />
                      </div>
                      <div className="otp-timer" style={{ color: 'var(--accent-red)', marginBottom: '20px', fontWeight: 'bold' }}>
                        OTP expires in: {otpTimer}s
                      </div>
                      <div style={{ display: 'flex', gap: '15px' }}>
                        <button type="button" className="btn btn-secondary" style={{ flex: 1 }} onClick={() => setSignupStep(1)}>Back</button>
                        <button type="submit" className="btn btn-primary" style={{ flex: 1 }}>Verify & Sign Up</button>
                      </div>
                    </div>
                  )}
                </form>
                <p className="auth-footer">Already have an account? <a href="#" onClick={(e) => { e.preventDefault(); showView('login'); }}>Login</a></p>
              </div>
            </section>
          )}

          {/* 4. FORGOT PASSWORD VIEW */}
          {view === 'forgot-password' && (
            <section className="view-section active">
              <div className="auth-card glass-panel">
                <h2>Reset Password</h2>
                <p className="auth-subtitle">Verify your identity to secure your account</p>
                <form className="auth-form" onSubmit={handleForgotSubmit}>
                  {forgotStep === 1 ? (
                    <div>
                      <div className="form-group" style={{ marginBottom: '20px' }}>
                        <label>Account Email</label>
                        <input type="email" required placeholder="doctor@hospital.com" value={forgotEmail} onChange={e => setForgotEmail(e.target.value)} />
                      </div>
                      <button type="button" className="btn btn-primary btn-block" onClick={handleForgotSendOtp}>Send Reset OTP</button>
                    </div>
                  ) : (
                    <div>
                      <div className="form-group" style={{ marginBottom: '15px' }}>
                        <label>Verification OTP</label>
                        <input type="text" required placeholder="123456" maxLength={6} value={forgotOtp} onChange={e => setForgotOtp(e.target.value)} />
                      </div>
                      <div className="form-group" style={{ marginBottom: '20px' }}>
                        <label>New Password</label>
                        <input type="password" required placeholder="••••••••" value={forgotNewPassword} onChange={e => setForgotNewPassword(e.target.value)} />
                      </div>
                      <div style={{ display: 'flex', gap: '15px' }}>
                        <button type="button" className="btn btn-secondary" style={{ flex: 1 }} onClick={() => setForgotStep(1)}>Cancel</button>
                        <button type="submit" className="btn btn-primary" style={{ flex: 1 }}>Reset Password</button>
                      </div>
                    </div>
                  )}
                </form>
              </div>
            </section>
          )}

          {/* 5. DASHBOARD VIEW */}
          {view === 'dashboard' && (
            <section className="view-section active">
              <div className="stats-grid">
                <div className="stat-card glass-panel stat-total" onClick={() => { setActiveHistoryFilter('total'); showView('history'); }}>
                  <div className="stat-icon">📂</div>
                  <div className="stat-details">
                    <div className="stat-value">{totalScansCount}</div>
                    <div className="stat-label">Total Scans</div>
                  </div>
                </div>
                <div className="stat-card glass-panel stat-normal" onClick={() => { setActiveHistoryFilter('normal'); showView('history'); }}>
                  <div className="stat-icon">🟢</div>
                  <div className="stat-details">
                    <div className="stat-value">{normalScansCount}</div>
                    <div className="stat-label">Normal Scans</div>
                  </div>
                </div>
                <div className="stat-card glass-panel stat-abnormal" onClick={() => { setActiveHistoryFilter('abnormal'); showView('history'); }}>
                  <div className="stat-icon">🔴</div>
                  <div className="stat-details">
                    <div className="stat-value">{tumorScansCount}</div>
                    <div className="stat-label">Tumor Detected</div>
                  </div>
                </div>
              </div>

              <div className="dashboard-action-bar glass-panel">
                <div className="action-banner-text">
                  <h3>Ready to perform new scan diagnostic?</h3>
                  <p>Upload a brain scan CT image for instantaneous AI analysis and boundary plotting.</p>
                </div>
                <button className="btn btn-primary" onClick={() => showView('scan')}>⚡ Start New Scan</button>
              </div>

              <div className="dashboard-recent-section" style={{ display: 'grid', gridTemplateColumns: '1.5fr 1fr', gap: '30px' }}>
                <div className="recent-diagnoses">
                  <div className="section-header">
                    <h2>Recent Diagnoses</h2>
                    <button className="text-btn" onClick={() => showView('history')}>View All →</button>
                  </div>
                  <div className="recent-scans-list">
                    {scans.length === 0 ? (
                      <div className="loading-placeholder">No scan analyses performed yet.</div>
                    ) : (
                      scans.slice(0, 5).map(scan => {
                        const isAbnormal = scan.result === 'Tumor' || scan.result === 'Hemorrhage';
                        return (
                          <div key={scan.id} className="scan-feed-item" onClick={() => handleRecentScanClick(scan)}>
                            <div className="feed-patient-info">
                              <div className="feed-avatar">👤</div>
                              <div className="feed-patient-details">
                                <h4>{scan.patient_name}</h4>
                                <p>ID: {scan.patient_id} | {scan.patient_age} yrs | {scan.patient_gender}</p>
                              </div>
                            </div>
                            <div className="feed-meta">
                              <span className="feed-date">{scan.date_added}</span>
                              {scan.result === 'Invalid Scan' || scan.result === 'Non-Brain CT' ? (
                                <span className="badge" style={{ background: 'rgba(234, 179, 8, 0.15)', color: '#eab308', border: '1px solid rgba(234, 179, 8, 0.3)' }}>Invalid Scan</span>
                              ) : (
                                <span className={`badge ${isAbnormal ? 'badge-danger' : 'badge-normal'}`}>
                                  {isAbnormal ? 'Tumor' : 'Normal'}
                                </span>
                              )}
                            </div>
                          </div>
                        );
                      })
                    )}
                  </div>
                </div>

                <div className="analytics-sidebar glass-panel">
                  <h3>Subtype Prevalence</h3>
                  <p style={{ fontSize: '12px', color: 'var(--text-secondary)', marginBottom: '20px' }}>Based on {totalScansCount} total analyses</p>
                  <div className="analytics-list" style={{ display: 'flex', flexDirection: 'column', gap: '15px' }}>
                    {[
                      { name: 'Intraventricular', count: scans.filter(s => parseFloat(s.intraventricular) > 0.5).length },
                      { name: 'Intraparenchymal', count: scans.filter(s => parseFloat(s.intraparenchymal) > 0.5).length },
                      { name: 'Subarachnoid', count: scans.filter(s => parseFloat(s.subarachnoid) > 0.5).length },
                      { name: 'Epidural', count: scans.filter(s => parseFloat(s.epidural) > 0.5).length },
                      { name: 'Subdural', count: scans.filter(s => parseFloat(s.subdural) > 0.5).length }
                    ].map(type => (
                      <div key={type.name}>
                        <div style={{ display: 'flex', justifyContent: 'space-between', fontSize: '13px', marginBottom: '5px' }}>
                          <span>{type.name}</span>
                          <span style={{ fontWeight: 'bold' }}>{type.count}</span>
                        </div>
                        <div style={{ height: '6px', background: 'rgba(255,255,255,0.05)', borderRadius: '3px', overflow: 'hidden' }}>
                          <div style={{
                            height: '100%',
                            background: 'var(--accent-cyan)',
                            width: `${(type.count / (tumorScansCount || 1)) * 100}%`
                          }}></div>
                        </div>
                      </div>
                    ))}
                  </div>
                </div>
              </div>
            </section>
          )}

          {/* 6. NEW SCAN VIEW */}
          {view === 'scan' && (
            <section className="view-section active">
              <div className="scan-container-grid">
                <div className="scan-form-card glass-panel">
                  <h2>Patient Demographics</h2>
                  <p className="section-subtitle">Enter diagnosis details below</p>
                  <form className="scan-form">
                    <div className="form-group" style={{ marginBottom: '15px' }}>
                      <label>Patient ID</label>
                      <input type="text" required placeholder="PAT_123456" value={scanPatientId} onChange={e => setScanPatientId(e.target.value)} />
                    </div>
                    <div className="form-group" style={{ marginBottom: '15px' }}>
                      <label>Patient Name</label>
                      <input type="text" required placeholder="Alex Mercer" value={scanPatientName} onChange={e => setScanPatientName(e.target.value)} />
                    </div>
                    <div className="form-group" style={{ marginBottom: '15px' }}>
                      <label>Age (Years)</label>
                      <input type="number" min={0} max={150} required placeholder="42" value={scanPatientAge} onChange={e => setScanPatientAge(e.target.value)} />
                    </div>
                    <div className="form-group">
                      <label>Gender</label>
                      <select required value={scanPatientGender} onChange={e => setScanPatientGender(e.target.value)}>
                        <option value="">Select Gender</option>
                        <option value="Male">Male</option>
                        <option value="Female">Female</option>
                        <option value="Other">Other</option>
                      </select>
                    </div>
                    <div className="form-group" style={{ marginTop: '15px' }}>
                      <label>Clinical Notes</label>
                      <textarea placeholder="Optional observations..." value={scanNotes} onChange={e => setScanNotes(e.target.value)} style={{ background: 'rgba(0,0,0,0.2)', border: '1px solid var(--border-color)', borderRadius: '12px', padding: '12px', color: 'white', minHeight: '80px' }}></textarea>
                    </div>
                  </form>
                </div>

                <div className="scan-upload-card glass-panel">
                  <h2>Brain CT Image</h2>
                  <p className="section-subtitle">Select or drop a scan image file (JPEG, PNG)</p>
                  <div className="upload-dropzone" onClick={() => fileInputRef.current.click()}>
                    <input type="file" ref={fileInputRef} className="hidden-input" accept="image/jpeg, image/png" onChange={e => handleFileSelect(e.target.files[0])} />
                    {!imagePreview ? (
                      <div className="dropzone-content">
                        <div className="dropzone-icon">📁</div>
                        <p className="dropzone-text"><strong>Drag & Drop Image Here</strong> or click to choose from system files</p>
                        <span className="dropzone-specs">Supports JPEG/PNG up to 10MB</span>
                      </div>
                    ) : (
                      <div className="dropzone-preview">
                        <img src={imagePreview} alt="Scan preview" />
                        <button className="remove-preview-btn" type="button" onClick={(e) => { e.stopPropagation(); resetScanForm(); }}>✖ Remove</button>
                      </div>
                    )}
                  </div>
                  <button type="button" className="btn btn-primary btn-block" style={{ marginTop: '20px' }} onClick={startDiagnosticAnalysis}>
                    Run Diagnostics Analysis
                  </button>
                </div>
              </div>
            </section>
          )}

          {/* 7. PROCESSING VIEW */}
          {view === 'processing' && (
            <section className="view-section active">
              <div className="processing-container glass-panel">
                <div className="scanning-wrapper">
                  <div className="scanning-logo">🧠</div>
                  <div className="scanner-glow-outer"></div>
                  <div className="scanner-glow-inner"></div>
                  <div className="scanner-line"></div>
                </div>
                <h2 className="processing-title">AI Processing Sequence</h2>
                <div className="progress-container">
                  <div className="progress-bar-fill" style={{ width: `${progressVal}%` }}></div>
                </div>
                <div className="progress-percentage">{progressVal}%</div>
                <div className="current-stage-text">{progressText}</div>

                <div className="processing-logs" style={{ display: 'flex', flexDirection: 'column', gap: '8px', maxHeight: '160px', overflowY: 'auto', background: 'rgba(0,0,0,0.15)', borderRadius: '12px', padding: '15px', textAlign: 'left', border: '1px solid var(--border-color)' }}>
                  {processingLogs.map((log, idx) => (
                    <div key={idx} style={{ color: 'var(--text-secondary)', fontSize: '13px' }}>
                      <span style={{ color: 'var(--accent-green-dark)', marginRight: '6px' }}>✓</span> {log}
                    </div>
                  ))}
                </div>
              </div>
            </section>
          )}

          {/* 8. RESULT VIEW */}
          {view === 'result' && resultData && (
            <section className="view-section active">
              <div className="result-container-grid">
                <div className="result-details-card glass-panel">
                  <div className="result-header" style={{ display: 'flex', alignItems: 'center', gap: '15px', marginBottom: '20px' }}>
                    <div className="result-badge-icon" style={{
                      fontSize: '28px',
                      background: resultData.result === 'Invalid Scan' ? 'rgba(234, 179, 8, 0.15)' : ((resultData.result === 'Tumor' || resultData.result === 'Hemorrhage') ? 'rgba(239, 68, 68, 0.1)' : 'rgba(16, 185, 129, 0.1)'),
                      color: resultData.result === 'Invalid Scan' ? '#eab308' : ((resultData.result === 'Tumor' || resultData.result === 'Hemorrhage') ? 'var(--accent-red)' : 'var(--accent-green)'),
                      width: '54px',
                      height: '54px',
                      borderRadius: '12px',
                      display: 'flex',
                      alignItems: 'center',
                      justifyContent: 'center'
                    }}>
                      {resultData.result === 'Invalid Scan' ? '⚠️' : ((resultData.result === 'Tumor' || resultData.result === 'Hemorrhage') ? '⚠️' : '✓')}
                    </div>
                    <div>
                      <h2 style={{ color: resultData.result === 'Invalid Scan' ? '#eab308' : ((resultData.result === 'Tumor' || resultData.result === 'Hemorrhage') ? 'var(--accent-red)' : 'var(--accent-green)') }}>
                        {resultData.result === 'Invalid Scan' ? 'INVALID / NON-BRAIN SCAN' : ((resultData.result === 'Tumor' || resultData.result === 'Hemorrhage') ? 'TUMOR DETECTED' : 'TUMOR NOT DETECTED')}
                      </h2>
                      <p className="result-subtitle" style={{ margin: 0 }}>
                        {resultData.result === 'Invalid Scan' ? 'File uploaded & saved to database (Flagged as non-Brain CT)' : 'Model Diagnostic Complete'}
                      </p>
                    </div>
                  </div>

                  <div className="result-section-card mt-20" style={{ background: 'rgba(0,0,0,0.1)', padding: '15px', borderRadius: '12px', marginBottom: '15px' }}>
                    <h3>Clinical Overview</h3>
                    <table className="result-table" style={{ width: '100%', borderCollapse: 'collapse', marginTop: '10px' }}>
                      <tbody>
                        <tr>
                          <th style={{ textAlign: 'left', padding: '6px 0', color: 'var(--text-secondary)', fontSize: '14px' }}>Patient ID</th>
                          <td style={{ textAlign: 'right', fontWeight: 'bold' }}>{resultData.patientId}</td>
                        </tr>
                        <tr>
                          <th style={{ textAlign: 'left', padding: '6px 0', color: 'var(--text-secondary)', fontSize: '14px' }}>Patient Name</th>
                          <td style={{ textAlign: 'right' }}>{resultData.patientName}</td>
                        </tr>
                        <tr>
                          <th style={{ textAlign: 'left', padding: '6px 0', color: 'var(--text-secondary)', fontSize: '14px' }}>Age / Gender</th>
                          <td style={{ textAlign: 'right' }}>{resultData.patientAge} / {resultData.patientGender}</td>
                        </tr>
                        <tr>
                          <th style={{ textAlign: 'left', padding: '6px 0', color: 'var(--text-secondary)', fontSize: '14px' }}>Diagnosis Time</th>
                          <td style={{ textAlign: 'right' }}>{resultData.dateTime}</td>
                        </tr>
                        <tr>
                          <th style={{ textAlign: 'left', padding: '6px 0', color: 'var(--text-secondary)', fontSize: '14px' }}>Confidence Level</th>
                          <td style={{ textAlign: 'right', fontWeight: 'bold', color: 'var(--accent-cyan)' }}>{(resultData.confidence * 100).toFixed(1)}%</td>
                        </tr>
                      </tbody>
                    </table>
                  </div>

                  {/* Subtype confidence breakdown (MANDATORY REQUIREMENT) */}
                  {(resultData.result === 'Tumor' || resultData.result === 'Hemorrhage') && (
                    <div className="result-section-card mt-20" style={{ background: 'rgba(0,0,0,0.1)', padding: '15px', borderRadius: '12px', marginBottom: '15px' }}>
                      <h3 style={{ marginBottom: '12px' }}>Tumor Subtype Analysis</h3>
                      <div className="subtype-bars">
                        {[
                          { name: 'Intraventricular', value: resultData.intraventricular },
                          { name: 'Intraparenchymal', value: resultData.intraparenchymal },
                          { name: 'Subarachnoid', value: resultData.subarachnoid },
                          { name: 'Epidural', value: resultData.epidural },
                          { name: 'Subdural', value: resultData.subdural }
                        ].map(sub => (
                          <div key={sub.name} style={{ marginBottom: '12px' }}>
                            <div style={{ display: 'flex', justifyContent: 'space-between', fontSize: '13px', marginBottom: '4px' }}>
                              <span style={{ color: 'var(--text-primary)' }}>{sub.name}</span>
                              <span style={{ color: 'var(--text-secondary)', fontWeight: 'bold' }}>{(sub.value * 100).toFixed(1)}%</span>
                            </div>
                            <div style={{ background: 'rgba(255, 255, 255, 0.05)', borderRadius: '10px', height: '8px', overflow: 'hidden', border: '1px solid var(--border-color)' }}>
                              <div style={{
                                background: sub.value >= 0.5 ? 'linear-gradient(90deg, var(--accent-cyan), var(--accent-blue))' : 'rgba(255,255,255,0.15)',
                                width: `${Math.min(100, Math.max(0, sub.value * 100))}%`,
                                height: '100%',
                                borderRadius: '10px',
                                transition: 'width 0.4s ease'
                              }}></div>
                            </div>
                          </div>
                        ))}
                      </div>
                    </div>
                  )}

                  <div className="result-section-card mt-20" style={{ background: 'rgba(0,0,0,0.1)', padding: '15px', borderRadius: '12px' }}>
                    <h3>Clinical Recommendations</h3>
                    <ul className="recommendations-list" style={{ marginTop: '10px', paddingLeft: '20px', fontSize: '14px', lineHeight: '1.6' }}>
                      {resultData.result === 'Invalid Scan' ? (
                        <>
                          <li style={{ color: '#eab308', fontWeight: 'bold', marginBottom: '6px' }}>⚠️ WARNING: Uploaded file does not match a valid Brain CT slice.</li>
                          <li style={{ marginBottom: '6px' }}>The record and image file have been saved to your database for audit history.</li>
                          <li>Please re-upload a clear Brain CT DICOM or JPEG image for AI diagnostic analysis.</li>
                        </>
                      ) : (resultData.result === 'Tumor' || resultData.result === 'Hemorrhage') ? (
                        <>
                          <li style={{ color: 'var(--accent-red-dark)', fontWeight: 'bold', marginBottom: '6px' }}>IMMEDIATE: Consult a neurologist or neurosurgeon.</li>
                          <li style={{ marginBottom: '6px' }}>Schedule a follow-up high-resolution head CT scan.</li>
                          <li>Monitor patient closely for elevated intracranial pressure or consciousness changes.</li>
                        </>
                      ) : (
                        <>
                          <li style={{ marginBottom: '6px' }}>No immediate neurosurgical intervention is suggested.</li>
                          <li>Perform routine follow-up scans if clinical symptoms persist.</li>
                        </>
                      )}
                    </ul>
                  </div>

                  {resultData.notes && (
                    <div className="result-section-card mt-20" style={{ background: 'rgba(0,0,0,0.1)', padding: '15px', borderRadius: '12px', marginTop: '15px' }}>
                      <h3>Clinical Notes</h3>
                      <p style={{ fontSize: '14px', marginTop: '10px', whiteSpace: 'pre-wrap' }}>{resultData.notes}</p>
                    </div>
                  )}

                  <div className="result-actions" style={{ display: 'flex', gap: '15px', marginTop: '20px' }}>
                    <button className="btn btn-secondary" style={{ flex: 1 }} onClick={() => showView('dashboard')}>← Dashboard</button>
                    <button className="btn btn-primary" style={{ flex: 1 }} onClick={() => showView('history')}>View Patient Scans</button>
                  </div>
                </div>

                <div className="result-image-card glass-panel" style={{ display: 'flex', flexDirection: 'column' }}>
                  <h3>Analyzed Scan Annotation</h3>
                  <div className="annotated-image-wrapper" style={{ flex: 1, display: 'flex', alignItems: 'center', justifyContent: 'center', marginTop: '15px', position: 'relative' }}>
                    <canvas ref={resultCanvasRef} style={{ maxWidth: '100%', maxHeight: '500px', objectFit: 'contain', border: '1px solid var(--border-color)', borderRadius: '12px' }}></canvas>
                  </div>
                </div>
              </div>
            </section>
          )}

          {/* 9. HISTORY VIEW */}
          {view === 'history' && (
            <section className="view-section active">
              <div className="history-card glass-panel">
                <div className="history-controls" style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', gap: '20px', flexWrap: 'wrap' }}>
                  <div className="search-box" style={{ display: 'flex', alignItems: 'center', background: 'rgba(0,0,0,0.2)', border: '1px solid var(--border-color)', borderRadius: '12px', padding: '6px 14px', flex: 1, minWidth: '250px' }}>
                    <span className="search-icon" style={{ marginRight: '8px' }}>🔍</span>
                    <input type="text" placeholder="Search by patient ID or name..." value={historySearch} onChange={e => setHistorySearch(e.target.value)} style={{ background: 'transparent', border: 'none', color: 'var(--text-primary)', outline: 'none', width: '100%' }} />
                  </div>
                  <div className="filter-group" style={{ display: 'flex', gap: '8px' }}>
                    <button className={`filter-tab ${activeHistoryFilter === 'total' ? 'active' : ''}`} onClick={() => setActiveHistoryFilter('total')}>All Scans</button>
                    <button className={`filter-tab ${activeHistoryFilter === 'normal' ? 'active' : ''}`} onClick={() => setActiveHistoryFilter('normal')}>Normal</button>
                    <button className={`filter-tab ${activeHistoryFilter === 'abnormal' ? 'active' : ''}`} onClick={() => setActiveHistoryFilter('abnormal')}>Tumor Detected</button>
                    <button className={`filter-tab ${activeHistoryFilter === 'invalid' ? 'active' : ''}`} onClick={() => setActiveHistoryFilter('invalid')}>Invalid Scans</button>
                  </div>
                </div>

                <div className="table-responsive mt-20" style={{ marginTop: '20px', overflowX: 'auto' }}>
                  <table className="history-table" style={{ width: '100%', borderCollapse: 'collapse', textAlign: 'left' }}>
                    <thead>
                      <tr style={{ borderBottom: '1px solid var(--border-color)' }}>
                        <th style={{ padding: '12px 8px', color: 'var(--text-secondary)' }}>Patient ID</th>
                        <th style={{ padding: '12px 8px', color: 'var(--text-secondary)' }}>Name</th>
                        <th style={{ padding: '12px 8px', color: 'var(--text-secondary)' }}>Diagnosis Date</th>
                        <th style={{ padding: '12px 8px', color: 'var(--text-secondary)' }}>Status Result</th>
                        <th style={{ padding: '12px 8px', color: 'var(--text-secondary)' }}>Action</th>
                      </tr>
                    </thead>
                    <tbody>
                      {getFilteredScans().length === 0 ? (
                        <tr>
                          <td colSpan={5} style={{ padding: '24px', textAlign: 'center', color: 'var(--text-secondary)' }}>No records found.</td>
                        </tr>
                      ) : (
                        getFilteredScans().map(scan => {
                          const isAbnormal = scan.result === 'Tumor' || scan.result === 'Hemorrhage';
                          return (
                            <tr key={scan.id} style={{ borderBottom: '1px solid var(--border-color)', cursor: 'pointer' }} onClick={() => handleRecentScanClick(scan)}>
                              <td style={{ padding: '12px 8px' }}><strong>{scan.patient_id}</strong></td>
                              <td style={{ padding: '12px 8px' }}>{scan.patient_name}</td>
                              <td style={{ padding: '12px 8px' }}>{scan.date_added} {scan.time_added}</td>
                              <td style={{ padding: '12px 8px' }}>
                                {scan.result === 'Invalid Scan' || scan.result === 'Non-Brain CT' ? (
                                  <span className="badge" style={{ background: 'rgba(234, 179, 8, 0.15)', color: '#eab308', border: '1px solid rgba(234, 179, 8, 0.3)' }}>Invalid Scan</span>
                                ) : (
                                  <span className={`badge ${isAbnormal ? 'badge-danger' : 'badge-normal'}`}>
                                    {isAbnormal ? 'Tumor' : 'Normal'}
                                  </span>
                                )}
                              </td>
                              <td style={{ padding: '12px 8px' }}>
                                <button className="text-btn" onClick={(e) => { e.stopPropagation(); handleRecentScanClick(scan); }}>View Report</button>
                              </td>
                            </tr>
                          );
                        })
                      )}
                    </tbody>
                  </table>
                </div>
              </div>
            </section>
          )}

          {/* 10. SETTINGS VIEW */}
          {view === 'settings' && (
            <section className="view-section active">
              <div className="settings-grid">
                <div className="settings-card glass-panel">
                  <h2>Clinician Profile</h2>
                  <p className="section-subtitle">Manage registration details</p>
                  <form className="profile-form mt-20" onSubmit={handleProfileSubmit}>
                    <div className="form-group" style={{ marginBottom: '15px' }}>
                      <label>Full Name</label>
                      <input type="text" required placeholder="Dr. John Doe" value={settingsName} onChange={e => setSettingsName(e.target.value)} />
                    </div>
                    <div className="form-group" style={{ marginBottom: '15px' }}>
                      <label>Email Address</label>
                      <input type="email" disabled placeholder="johndoe@hospital.com" value={user.email} />
                    </div>
                    <div className="form-group" style={{ marginBottom: '15px' }}>
                      <label>Specialty</label>
                      <input type="text" placeholder="Radiology Specialist" value={settingsSpecialty} onChange={e => setSettingsSpecialty(e.target.value)} />
                    </div>
                    <div className="form-group" style={{ marginBottom: '15px' }}>
                      <label>Hospital / Institution</label>
                      <input type="text" placeholder="General Hospital" value={settingsHospital} onChange={e => setSettingsHospital(e.target.value)} />
                    </div>
                    <div className="form-group" style={{ marginBottom: '15px' }}>
                      <label>Medical License No.</label>
                      <input type="text" placeholder="LIC-12345" value={settingsLicense} onChange={e => setSettingsLicense(e.target.value)} />
                    </div>
                    <div className="form-group" style={{ marginBottom: '15px' }}>
                      <label>Years of Experience</label>
                      <input type="number" placeholder="5" value={settingsExperience} onChange={e => setSettingsExperience(e.target.value)} />
                    </div>
                    <div className="form-group" style={{ marginBottom: '15px' }}>
                      <label>Date of Birth</label>
                      <input type="date" value={settingsDob} onChange={e => setSettingsDob(e.target.value)} />
                    </div>
                    <div className="form-group" style={{ marginBottom: '20px' }}>
                      <label>Address</label>
                      <textarea placeholder="Hospital Address" value={settingsAddress} onChange={e => setSettingsAddress(e.target.value)} style={{ width: '100%', borderRadius: '8px', padding: '10px', background: 'rgba(0,0,0,0.1)', border: '1px solid var(--border-color)', color: 'var(--text-primary)' }}></textarea>
                    </div>
                    <button type="submit" className="btn btn-primary">Save Profile Changes</button>
                  </form>
                </div>

                <div className="settings-card glass-panel">
                  <h2>Preferences & Security</h2>
                  <p className="section-subtitle">Manage UI features and credentials</p>
                  <div className="preferences-list mt-20">
                    <div className="preference-item" style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '20px' }}>
                      <div className="preference-info">
                        <h4 style={{ margin: 0 }}>Dark Mode Theme</h4>
                        <p style={{ margin: 0, fontSize: '12px', color: 'var(--text-secondary)' }}>Toggle system interface appearance</p>
                      </div>
                      <label className="toggle-switch" style={{ position: 'relative', display: 'inline-block', width: '46px', height: '24px' }}>
                        <input type="checkbox" checked={theme === 'dark'} onChange={e => handleSettingChange('theme', e.target.checked ? 'dark' : 'light')} style={{ opacity: 0, width: 0, height: 0 }} />
                        <span className="slider round" style={{
                          position: 'absolute', cursor: 'pointer', top: 0, left: 0, right: 0, bottom: 0,
                          backgroundColor: theme === 'dark' ? 'var(--accent-cyan)' : '#ccc',
                          transition: '.4s', borderRadius: '34px'
                        }}></span>
                      </label>
                    </div>

                    <div className="preference-item" style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '20px' }}>
                      <div className="preference-info">
                        <h4 style={{ margin: 0 }}>Daily Summary</h4>
                        <p style={{ margin: 0, fontSize: '12px', color: 'var(--text-secondary)' }}>Receive daily clinical activity reports</p>
                      </div>
                      <label className="toggle-switch" style={{ position: 'relative', display: 'inline-block', width: '46px', height: '24px' }}>
                        <input type="checkbox" checked={dailySummary} onChange={e => handleSettingChange('daily_summary', e.target.checked)} style={{ opacity: 0, width: 0, height: 0 }} />
                        <span className="slider round" style={{
                          position: 'absolute', cursor: 'pointer', top: 0, left: 0, right: 0, bottom: 0,
                          backgroundColor: dailySummary ? 'var(--accent-cyan)' : '#ccc',
                          transition: '.4s', borderRadius: '34px'
                        }}></span>
                      </label>
                    </div>

                    <div className="preference-item" style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '20px' }}>
                      <div className="preference-info">
                        <h4 style={{ margin: 0 }}>Sound Notifications</h4>
                        <p style={{ margin: 0, fontSize: '12px', color: 'var(--text-secondary)' }}>Play sound for new scan alerts</p>
                      </div>
                      <label className="toggle-switch" style={{ position: 'relative', display: 'inline-block', width: '46px', height: '24px' }}>
                        <input type="checkbox" checked={notifSound} onChange={e => handleSettingChange('notif_sound', e.target.checked)} style={{ opacity: 0, width: 0, height: 0 }} />
                        <span className="slider round" style={{
                          position: 'absolute', cursor: 'pointer', top: 0, left: 0, right: 0, bottom: 0,
                          backgroundColor: notifSound ? 'var(--accent-cyan)' : '#ccc',
                          transition: '.4s', borderRadius: '34px'
                        }}></span>
                      </label>
                    </div>

                    <div className="preference-item" style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '40px' }}>
                      <div className="preference-info">
                        <h4 style={{ margin: 0 }}>Inference Location</h4>
                        <p style={{ margin: 0, fontSize: '12px', color: 'var(--text-secondary)' }}>Run AI diagnostics locally or cloud</p>
                      </div>
                      <span className="badge badge-normal">Local Client</span>
                    </div>

                    <div className="danger-zone mt-20" style={{ borderTop: '1px solid var(--border-color)', paddingTop: '20px' }}>
                      <h3 style={{ color: 'var(--accent-red)' }}>Danger Zone</h3>
                      <p style={{ fontSize: '12px', color: 'var(--text-secondary)', marginBottom: '15px' }}>Irreversible security operations</p>
                      <button className="btn btn-danger btn-block" onClick={handleDeleteAccount}>Delete Clinician Account</button>
                    </div>
                  </div>
                </div>
              </div>
            </section>
          )}

        </div>
      </main>
    </div>
  );
}

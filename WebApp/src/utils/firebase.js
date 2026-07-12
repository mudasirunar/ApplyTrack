import { initializeApp } from 'firebase/app';
import { getAuth } from 'firebase/auth';
import { getFirestore } from 'firebase/firestore';
import { getAnalytics, isSupported } from 'firebase/analytics';
import { config } from './config';

// Initialize Firebase
const app = initializeApp(config.firebase);

// Export Auth and Firestore services
export const auth = getAuth(app);
export const firestore = getFirestore(app);

// Export Analytics
export let analytics = null;
isSupported().then((supported) => {
  if (supported) {
    analytics = getAnalytics(app);
  }
});

export default app;

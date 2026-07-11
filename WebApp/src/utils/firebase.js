import { initializeApp } from 'firebase/app';
import { getAuth } from 'firebase/auth';
import { getFirestore } from 'firebase/firestore';
import { config } from './config';

// Initialize Firebase
const app = initializeApp(config.firebase);

// Export Auth and Firestore services
export const auth = getAuth(app);
export const firestore = getFirestore(app);
export default app;

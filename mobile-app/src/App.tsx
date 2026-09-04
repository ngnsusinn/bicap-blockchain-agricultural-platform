import React, { useState } from 'react';
import './App.css';
import { UserSession, Shipment } from './types';
import { getCurrentSession, logoutDriver } from './utils/api';
import { LoginScreen } from './components/LoginScreen';
import { ShipmentListScreen } from './components/ShipmentListScreen';
import { TrackingUpdateScreen } from './components/TrackingUpdateScreen';
import { QRScanScreen } from './components/QRScanScreen';
import { PickupConfirmScreen } from './components/PickupConfirmScreen';
import { DeliveryConfirmScreen } from './components/DeliveryConfirmScreen';
import { ReportScreen } from './components/ReportScreen';

export default function App() {
  const [session, setSession] = useState<UserSession | null>(getCurrentSession());
  const [currentScreen, setCurrentScreen] = useState<'list' | 'tracking' | 'qr' | 'pickup' | 'deliver' | 'report'>('list');
  const [selectedShipment, setSelectedShipment] = useState<Shipment | null>(null);
  const [showPublicQRScan, setShowPublicQRScan] = useState<boolean>(false);

  const handleLogout = () => {
    logoutDriver();
    setSession(null);
    setCurrentScreen('list');
    setSelectedShipment(null);
    setShowPublicQRScan(false);
  };

  const handleSelectShipment = (shipment: Shipment, action: 'detail' | 'tracking' | 'qr' | 'pickup' | 'deliver' | 'report') => {
    setSelectedShipment(shipment);
    if (action !== 'detail') {
      setCurrentScreen(action);
    }
  };

  if (!session) {
    if (showPublicQRScan) {
      return (
        <div className="app-container">
          <QRScanScreen onBack={() => setShowPublicQRScan(false)} />
        </div>
      );
    }
    return (
      <LoginScreen
        onLoginSuccess={setSession}
        onOpenPublicQRScan={() => setShowPublicQRScan(true)}
      />
    );
  }

  return (
    <>
      <header className="top-navbar">
        <div className="brand">
          <span>🚚</span>
          <span>BICAP Driver</span>
        </div>
        <div className="user-info">
          <span className="user-name">👤 {session.fullName}</span>
          <button onClick={handleLogout} className="btn-logout">Đăng xuất</button>
        </div>
      </header>

      {currentScreen === 'list' && (
        <ShipmentListScreen onSelectShipment={handleSelectShipment} />
      )}

      {currentScreen === 'tracking' && selectedShipment && (
        <TrackingUpdateScreen
          shipment={selectedShipment}
          onBack={() => setCurrentScreen('list')}
          onSuccess={() => setCurrentScreen('list')}
        />
      )}

      {currentScreen === 'qr' && (
        <QRScanScreen
          shipment={selectedShipment || undefined}
          onBack={() => setCurrentScreen('list')}
        />
      )}

      {currentScreen === 'pickup' && selectedShipment && (
        <PickupConfirmScreen
          shipment={selectedShipment}
          onBack={() => setCurrentScreen('list')}
          onSuccess={() => setCurrentScreen('list')}
        />
      )}

      {currentScreen === 'deliver' && selectedShipment && (
        <DeliveryConfirmScreen
          shipment={selectedShipment}
          onBack={() => setCurrentScreen('list')}
          onSuccess={() => setCurrentScreen('list')}
        />
      )}

      {currentScreen === 'report' && (
        <ReportScreen
          shipment={selectedShipment || undefined}
          onBack={() => setCurrentScreen('list')}
          onSuccess={() => setCurrentScreen('list')}
        />
      )}
    </>
  );
}

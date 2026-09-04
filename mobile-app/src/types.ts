export interface UserSession {
  token: string;
  id: number;
  email: string;
  fullName: string;
  role: string;
  phone?: string;
}

export interface ShipmentTracking {
  id: number;
  shipmentId: number;
  timestamp: string;
  status: string;
  gpsLat: number;
  gpsLng: number;
  images?: string[];
  notes?: string;
}

export interface ShipmentItem {
  productName: string;
  quantity: number;
  unit: string;
}

export interface Shipment {
  id: number;
  shipmentCode?: string;
  orderId: number;
  orderCode?: string;
  farmName?: string;
  farmAddress?: string;
  farmPhone?: string;
  retailerName?: string;
  retailerAddress?: string;
  deliveryAddr?: string;
  retailerPhone?: string;
  status: string;
  pickupTime?: string;
  deliveryTime?: string;
  licensePlate?: string;
  vehicleLicensePlate?: string;
  driverName?: string;
  driverPhone?: string;
  routeSummary?: string;
  items?: ShipmentItem[];
  trackingList?: ShipmentTracking[];
  trackingHistory?: ShipmentTracking[];
}

export interface DriverReportRequest {
  shipmentId: number;
  reportType: string;
  description: string;
  images?: string[];
  gpsLat?: number;
  gpsLng?: number;
}

export interface PickupConfirmRequest {
  gpsLat: number;
  gpsLng: number;
  images?: string[];
  notes?: string;
}

export interface DeliveryConfirmRequest {
  gpsLat: number;
  gpsLng: number;
  images: string[];
  notes?: string;
  recipientSignature?: string;
}

export interface TrackingAddRequest {
  status: string;
  gpsLat: number;
  gpsLng: number;
  images?: string[];
  notes?: string;
}

/**
 * Dịch mã loại quy trình sản xuất từ tiếng Anh sang tiếng Việt.
 * Backend lưu mã tiếng Anh (SOIL_PREP, SEEDING...) cho thống nhất API,
 * frontend hiển thị tên tiếng Việt cho người dùng.
 */

export const PROCESS_TYPE_LABELS: Record<string, string> = {
  SOIL_PREP: "Chuẩn bị đất",
  SEEDING: "Gieo hạt",
  FERTILIZATION: "Bón phân",
  PEST_CONTROL: "Phòng trừ sâu bệnh",
  HARVESTING: "Thu hoạch",
};

export const SEASON_STATUS_LABELS: Record<string, string> = {
  IN_PROGRESS: "Đang thực hiện",
  HARVESTED: "Đã thu hoạch",
  CANCELLED: "Đã hủy",
  PENDING: "Chờ xử lý",
  APPROVED: "Đã duyệt",
};

/**
 * Dịch mã loại quy trình sang tiếng Việt.
 * Nếu không có bản dịch, trả về mã gốc.
 */
export function translateProcessType(code: string | null | undefined): string {
  if (!code) return "—";
  return PROCESS_TYPE_LABELS[code] || code;
}

/**
 * Dịch mã trạng thái mùa vụ sang tiếng Việt.
 * Nếu không có bản dịch, trả về mã gốc.
 */
export function translateSeasonStatus(code: string | null | undefined): string {
  if (!code) return "—";
  return SEASON_STATUS_LABELS[code] || code;
}


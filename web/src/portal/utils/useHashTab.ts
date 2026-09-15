import { useCallback, useEffect, useState } from 'react';

/**
 * Tab điều hướng của portal được lưu vào hash của URL (`#<namespace>/<tab>`).
 *
 * Nhờ vậy khi người dùng F5 / tải lại trang thì vẫn ở đúng màn hình đang xem
 * (ví dụ đang ở "Cập nhật hồ sơ" thì reload vẫn vào "Cập nhật hồ sơ"), và có thể
 * copy link để mở thẳng màn hình đó. Hash không gửi lên server nên không ảnh hưởng
 * tới Spring Boot static shell hay `SpaForwardController`.
 */

/** Đọc tab hợp lệ từ hash hiện tại; trả `null` nếu hash trống/sai namespace/tab lạ. */
export function readHashTab(validTabs: readonly string[], namespace: string): string | null {
  const raw = window.location.hash.replace(/^#/, '').trim();
  if (!raw) return null;
  const [ns, tab] = raw.split('/');
  if (ns !== namespace || !tab) return null;
  return validTabs.includes(tab) ? tab : null;
}

/**
 * @param validTabs danh sách tab hợp lệ (nên là hằng số ở module để không đổi identity mỗi render)
 * @param fallback  tab mặc định khi URL chưa có hash
 * @param namespace tiền tố trong hash, tách biệt giữa các portal (farm/retailer)
 */
export default function useHashTab(
  validTabs: readonly string[],
  fallback: string,
  namespace: string,
): [string, (tab: string) => void] {
  const [tab, setTabState] = useState<string>(
    () => readHashTab(validTabs, namespace) ?? fallback,
  );

  const setTab = useCallback(
    (next: string) => {
      setTabState(next);
      const nextHash = `#${namespace}/${next}`;
      if (window.location.hash !== nextHash) {
        // replaceState: đổi hash mà không tạo thêm entry trong history (nút Back vẫn thoát portal).
        window.history.replaceState(null, '', nextHash);
      }
    },
    [namespace],
  );

  // Hỗ trợ đổi hash từ bên ngoài (dán link, sửa hash, back/forward).
  useEffect(() => {
    const onHashChange = () => {
      const fromHash = readHashTab(validTabs, namespace);
      if (fromHash) setTabState(fromHash);
    };
    window.addEventListener('hashchange', onHashChange);
    return () => window.removeEventListener('hashchange', onHashChange);
  }, [namespace, validTabs]);

  return [tab, setTab];
}

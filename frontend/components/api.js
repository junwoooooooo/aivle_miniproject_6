// 같은 오리진 상대경로로 호출. 배포: nginx가 /books,/reviews,/auth,/admin 을 backend-service로 프록시.
// 로컬 dev: vite proxy가 동일 경로를 localhost:8080으로 전달.
export const API_BASE_URL = "";

export async function request(path, options = {}) {
  const token = localStorage.getItem('token');

  const res = await fetch(`${API_BASE_URL}${path}`, {
    headers: {
      "Content-Type": "application/json",
      ...(token ? { "Authorization": `Bearer ${token}` } : {}),
      ...(options.headers || {}),
    },
    ...options,
  });

  if (!res.ok) {
    throw new Error(`요청 실패: ${res.status}`);
  }

  const text = await res.text();
  return text ? JSON.parse(text) : null;
}
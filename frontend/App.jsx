import { useEffect, useState } from 'react';
import { Routes, Route, Link, useNavigate, useLocation } from 'react-router-dom';
import './App.css';

import HomePage from './pages/HomePage';
import ListPage from './pages/ListPage';
import CreatePage from './pages/CreatePage';
import DetailPage from './pages/DetailPage';
import ReviewListPage from './pages/ReviewListPage';
import LoginPage from './pages/LoginPage';
import SignupPage from './pages/SignupPage';
import MyPage from './pages/MyPage';
import AdminPage from './pages/AdminPage';
import { request } from './components/api.js';

function App() {
  const navigate = useNavigate();
  const location = useLocation();

  const [books, setBooks] = useState([]);
  const [reviews, setReviews] = useState([]);
  const [loading, setLoading] = useState(true);
  const [tags, setTags] = useState([]);
  const [currentUser, setCurrentUser] = useState(
    localStorage.getItem('username') || null
  );

  const handleLogin = (username) => {
    setCurrentUser(username);
  };

  const handleLogout = () => {
    localStorage.removeItem('token');
    localStorage.removeItem('username');
    localStorage.removeItem('role');
    setCurrentUser(null);
      if (location.pathname === '/admin' || location.pathname === '/mypage') {
      navigate('/');
    }
  };

  useEffect(() => {
    async function loadData() {
      try {
        const booksRes = await request('/books');
        const reviewsRes = await request('/reviews');
        setBooks(booksRes);
        setReviews(reviewsRes);
      } catch (err) {
        console.error('데이터 불러오기 실패:', err);
      }
      setLoading(false);
    }
    loadData();
  }, []);

  useEffect(() => {
    setTags([...new Set(books.flatMap(b => b.tags ? b.tags : []))]);
  }, [books]);

  const handleCreateBook = async (newBook) => {
      const savedBook = await request('/books', {
          method: 'POST',
          body: JSON.stringify(newBook),
      });
      setBooks([savedBook, ...books]);
      return savedBook;
  };

  const handleView = async (id) => {
    try {
      const updated = await request(`/books/${id}/views`, {
        method: 'PATCH',
      });
      setBooks((prevBooks) => prevBooks.map((book) => String(book.id) === String(id) ? updated : book));
    } catch (err) {
      console.error('handleView Error:', err);
    }
  };

  const handleBookLikes = async (id) => {
    const token = localStorage.getItem('token');
    if (!token) {
      alert('로그인이 필요합니다.');
      return;
    }

    const likedBooks = JSON.parse(localStorage.getItem('likedBooks') || '[]');
    const already = likedBooks.includes(String(id));

    try {
      const book = books.find(b => String(b.id) === String(id));
      const updatedBook = await request(`/books/${id}/likes`, {
        method: 'PATCH',
        body: JSON.stringify({
          likes: already ? Math.max(0, (book.likes ?? 0) - 1) : (book.likes ?? 0) + 1
        }),
      });
      setBooks((prevBooks) =>
        prevBooks.map((b) => String(b.id) === String(id) ? updatedBook : b)
      );
      if (already) {
        localStorage.setItem('likedBooks', JSON.stringify(likedBooks.filter(i => i !== String(id))));
      } else {
        localStorage.setItem('likedBooks', JSON.stringify([...likedBooks, String(id)]));
      }
    } catch (err) {
      console.error(err);
    }
  };
    
  const handleReviewLike = async (id) => {
    const token = localStorage.getItem('token');
    if (!token) {
      alert('로그인이 필요합니다.');
      return;
    }

    const likedReviews = JSON.parse(localStorage.getItem('likedReviews') || '[]');
    const already = likedReviews.includes(String(id));

    try {
      const review = reviews.find(r => r.id === id);
      const updated = await request(`/reviews/${id}/likes`, {
        method: 'PATCH',
        body: JSON.stringify({
          likes: already ? Math.max(0, (review.likes ?? 0) - 1) : (review.likes ?? 0) + 1
        }),
      });
      setReviews(reviews.map((r) => (r.id === id ? updated : r)));
      if (already) {
        localStorage.setItem('likedReviews', JSON.stringify(likedReviews.filter(i => i !== String(id))));
      } else {
        localStorage.setItem('likedReviews', JSON.stringify([...likedReviews, String(id)]));
      }
    } catch (err) {
      console.error(err);
    }
  };

  const handleReviewEdit = async (id, edited) => {
    const updated = await request(`/reviews/${id}`, {
      method: 'PATCH',
      body: JSON.stringify(edited),
    });
    setReviews(reviews.map(r => String(r.id) === String(id) ? updated : r));
    return updated;
  };

  const handleBookEdit = async (id, edited) => {
      try {
          const updated = await request(`/books/${id}`, {
              method: 'PATCH',
              body: JSON.stringify(edited),
          });
          setBooks(books.map(b => String(b.id) === String(id) ? updated : b));
      } catch (err) {
          console.error(err);
      }
  };

  const handleBookDelete = async (id) => {
      try {
          await request(`/books/${id}`, { method: 'DELETE' });
          setBooks(books.filter(b => String(b.id) !== String(id)));
      } catch (err) {
          console.error(err);
      }
  };

  const handleReviewDelete = async (id) => {
    try {
      await request(`/reviews/${id}`, { method: 'DELETE' });
      setReviews(reviews.filter(r => r.id !== id));
    } catch (err) {
      console.error(err);
    }
  };

  const handleReviewAdd = (saved) => {
    try {
      setReviews([...reviews, saved]);
    } catch {
      alert('새로고침 필요.');
    }
  };

  if (loading) {
    return <div>전체 데이터를 불러오는 중입니다...</div>;
  }

  return (
    <div className="app-shell">
      <header className="header">
        <Link to="/" className="logo" aria-label="홈으로 이동">
          <span className="logo-icon">📚</span>
          <h1>도서관리</h1>
        </Link>

        <nav aria-label="주요 메뉴">
          {currentUser && localStorage.getItem('role') === 'ADMIN' && (
            <Link to="/admin">관리자</Link>
          )}
          <Link to="/">홈</Link>
          <Link to="/list">도서목록</Link>
          <Link to="/reviews">리뷰목록</Link>
          <Link to="/create" className="add-book-btn">+ 새 도서 등록</Link>
          {currentUser ? (
            <>
              <Link to="/mypage" className="nav-username">{currentUser}님</Link>
              <button className="nav-logout-btn" onClick={handleLogout}>로그아웃</button>
            </>
          ) : (
            <>
              <Link to="/login" className="nav-pill-btn">로그인</Link>
              <Link to="/signup" className="nav-pill-btn">회원가입</Link>
            </>
          )}
        </nav>
      </header>

      <main className="container">
        <Routes>
          <Route path="/" element={<HomePage books={books} reviews={reviews} />} />
          <Route path="/list" element={<ListPage books={books} tags={tags} />} />
          <Route path="/create" element={<CreatePage onCreateBook={handleCreateBook} />} />
          <Route path="/detail/:id" element={
            <DetailPage
              books={books}
              reviews={reviews}
              onBookEdit={handleBookEdit}
              onBookDelete={handleBookDelete}
              onBookLikes={handleBookLikes}
              onReviewLike={handleReviewLike}
              onReviewEdit={handleReviewEdit}
              onReviewDelete={handleReviewDelete}
              onReviewAdd={handleReviewAdd}
              onView={handleView}
            />}
          />
          <Route path="/reviews" element={<ReviewListPage reviews={reviews} books={books} />} />
          <Route path="/login" element={<LoginPage onLogin={handleLogin} />} />
          <Route path="/signup" element={<SignupPage />} />
          <Route path="/mypage" element={<MyPage />} />
          <Route path="/admin" element={<AdminPage />} />
        </Routes>
      </main>

      <footer className="footer">
        <div className="footer-inner">
          <div className="footer-brand">
            <span className="footer-logo">📚</span>
            <div>
              <strong>도서관리</strong>
              <p>AI 표지 생성과 리뷰 기능을 제공하는 도서 관리 서비스입니다.</p>
            </div>
          </div>

          <div className="footer-info">
            <div className="footer-info-item">
              <span className="footer-info-title">서비스 이용</span>
              <p>도서 등록, 리뷰 작성, 좋아요 기능은 로그인 후 이용할 수 있습니다.</p>
            </div>

            <div className="footer-info-item">
              <span className="footer-info-title">프로젝트 정보</span>
              <p>AIVLE Mini Project 6 · React & Spring Boot 기반 도서관리 시스템</p>
            </div>
          </div>

          <div className="footer-copy">
            © 2026 AIVLE Mini Project 6. All rights reserved.
          </div>
        </div>
      </footer>
    </div>
  );
}

export default App;
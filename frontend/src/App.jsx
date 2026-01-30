import { useEffect, useState } from 'react'
import './App.css'

async function apiFetch(path, options = {}) {
  const res = await fetch(`/api${path}`, {
    headers: { 'Content-Type': 'application/json', ...(options.headers || {}) },
    ...options,
  })

  const contentType = res.headers.get('content-type') || ''
  let payload = null

  if (contentType.includes('application/json')) {
    try {
      payload = await res.json()
    } catch {
      // ignore
    }
  }

  if (!res.ok) {
    const message = payload?.error || payload?.message || `Request failed (${res.status})`
    const err = new Error(message)
    err.status = res.status
    throw err
  }

  return payload
}

export default function App() {
  const [fullUrl, setFullUrl] = useState('')
  const [customAlias, setCustomAlias] = useState('')
  const [shortUrl, setShortUrl] = useState('')
  const [error, setError] = useState('')
  const [urls, setUrls] = useState([])
  const [loading, setLoading] = useState(false)
  const [refreshing, setRefreshing] = useState(false)

  async function loadUrls() {
    const data = await apiFetch('/urls', { method: 'GET' })
    setUrls(Array.isArray(data) ? data : [])
  }

  useEffect(() => {
    loadUrls().catch((e) => setError(e.message))
  }, [])

  async function onSubmit(e) {
    e.preventDefault()
    setError('')
    setShortUrl('')
    setLoading(true)

    try {
      const body = {
        fullUrl: fullUrl.trim(),
        ...(customAlias.trim() ? { customAlias: customAlias.trim() } : {}),
      }

      const res = await apiFetch('/shorten', {
        method: 'POST',
        body: JSON.stringify(body),
      })

      setShortUrl(res.shortUrl)
      setFullUrl('')
      setCustomAlias('')
      await loadUrls()
    } catch (e2) {
      setError(e2.message)
    } finally {
      setLoading(false)
    }
  }

  async function onRefresh() {
    setError('')
    setRefreshing(true)
    try {
      await loadUrls()
    } catch (e) {
      setError(e.message)
    } finally {
      setRefreshing(false)
    }
  }

  async function onDelete(alias) {
    setError('')
    try {
      await apiFetch(`/${encodeURIComponent(alias)}`, { method: 'DELETE' })
      await loadUrls()
    } catch (e) {
      setError(e.message)
    }
  }

  return (
    <div className="container">
      <h1>URL Shortener</h1>
      <p className="subtitle">Enter a URL to shorten it. Optionally provide a custom alias.</p>

      <form onSubmit={onSubmit} className="form">
        <label className="field">
          <span className="label">Full URL</span>
          <input
            value={fullUrl}
            onChange={(e) => setFullUrl(e.target.value)}
            placeholder="https://example.com/very/long/url"
            required
          />
        </label>

        <label className="field">
          <span className="label">Custom alias (optional)</span>
          <input
            value={customAlias}
            onChange={(e) => setCustomAlias(e.target.value)}
            placeholder="my-custom-alias"
          />
          <small className="help">Allowed: letters, numbers, hyphens. Length: 3–50.</small>
        </label>

        <button type="submit" disabled={loading} className="primaryBtn">
          {loading ? 'Shortening…' : 'Shorten URL'}
        </button>
      </form>

      {error && (
        <div className="error">
          <strong>Error:</strong> {error}
        </div>
      )}

      {shortUrl && (
        <div className="success">
          <strong>Short URL:</strong>{' '}
          <a href={shortUrl} target="_blank" rel="noreferrer">
            {shortUrl}
          </a>
        </div>
      )}

      <hr />

      <div className="savedHeader">
        <h2>Saved URLs</h2>
        <button onClick={onRefresh} disabled={refreshing} className="secondaryBtn">
          {refreshing ? 'Refreshing…' : 'Refresh list'}
        </button>
      </div>

      <div className="urlList">
        {urls.length === 0 ? (
          <p className="muted">No URLs saved yet.</p>
        ) : (
          urls.map((u) => (
            <div key={u.alias} className="urlCard">
              <div className="urlCardHeader">
                <div className="urlInfo">
                  <div>
                    <strong>Alias:</strong> {u.alias}
                  </div>
                  <div className="rowTop">
                    <strong>Short:</strong>{' '}
                    <a href={u.shortUrl} target="_blank" rel="noreferrer">
                      {u.shortUrl}
                    </a>
                  </div>
                </div>

                <div className="urlFull">
                  <div>
                    <strong>Full:</strong>{' '}
                    <a href={u.fullUrl} target="_blank" rel="noreferrer">
                      {u.fullUrl}
                    </a>
                  </div>
                </div>

                <div className="urlActions">
                  <button onClick={() => onDelete(u.alias)} className="deleteBtn">
                    Delete
                  </button>
                </div>
              </div>
            </div>
          ))
        )}
      </div>

      <p className="note">
        Note: In development, the frontend proxies API requests via Vite (<code>/api</code>) to avoid CORS complexity.
      </p>
    </div>
  )
}

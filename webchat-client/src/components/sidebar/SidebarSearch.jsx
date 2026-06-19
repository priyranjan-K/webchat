import React from 'react'

/**
 * SidebarSearch — search input with icon.
 */
function SidebarSearch({ value, onChange, placeholder = 'Search…' }) {
  return (
    <div className="sidebar-search">
      <div className="search-wrap">
        <span className="search-icon">🔍</span>
        <input
          id="contact-search"
          type="search"
          placeholder={placeholder}
          value={value}
          onChange={(e) => onChange(e.target.value)}
          autoComplete="off"
        />
      </div>
    </div>
  )
}

export default SidebarSearch

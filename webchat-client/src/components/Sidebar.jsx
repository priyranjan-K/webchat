import React, { useState } from 'react'
import { Link } from 'react-router-dom'
import SidebarSearch  from './sidebar/SidebarSearch'
import SidebarFooter  from './sidebar/SidebarFooter'
import GroupList      from './sidebar/GroupList'
import OnlineUsers    from './sidebar/OnlineUsers'
import { chatService } from '../services/chatService'

const TABS = ['Groups', 'People']

/**
 * Sidebar — left panel orchestrator.
 *
 * Tabs:
 *  - Groups: chat rooms (create / remove / leave)
 *  - People: contacts for DM (add / remove)
 *
 * Props:
 *  onRemoveGroup(group)  — called when user clicks X on a group
 *  onRemoveDm(phone)     — called when user clicks X on a contact
 */
function Sidebar({
  activeGroupId, activeDmPhone,
  onSelectGroup, onSelectUser,
  displayName, userPhone, isConnected,
  onLogout,
  groups = [], loadGroups,
  allUsers = [], loadUsers,
  onlineUsers = [],
  onRemoveGroup,
  onRemoveDm,
  unreadCounts = {},
}) {
  const [search,           setSearch]           = useState('')
  const [activeTab,        setActiveTab]        = useState('Groups')
  const [newGroupName,     setNewGroupName]     = useState('')
  const [showCreate,       setShowCreate]       = useState(false)
  const [creating,         setCreating]         = useState(false)
  const [showAddContact,   setShowAddContact]   = useState(false)
  const [newContactPhone,  setNewContactPhone]  = useState('')
  const [contactError,     setContactError]     = useState('')
  const [addingContact,    setAddingContact]    = useState(false)

  // ─── Create group ─────────────────────────────────────────────
  const handleCreateGroup = async (e) => {
    e.preventDefault()
    if (!newGroupName.trim() || !isConnected) return
    setCreating(true)
    try {
      await chatService.createGroup(newGroupName.trim())
      setNewGroupName('')
      setShowCreate(false)
    } finally {
      setCreating(false)
    }
  }

  // ─── Add contact ──────────────────────────────────────────────
  const handleAddContact = async (e) => {
    e.preventDefault()
    const targetPhone = newContactPhone.trim()
    if (!targetPhone || !isConnected) return
    if (targetPhone === userPhone) {
      setContactError("You cannot add yourself!")
      return
    }
    setAddingContact(true)
    setContactError('')
    try {
      await chatService.addContact(targetPhone)
      setNewContactPhone('')
      setShowAddContact(false)
      loadUsers()
      onSelectUser(targetPhone)
    } catch (err) {
      const msg = err.response?.data || "Failed to add contact"
      setContactError(msg)
    } finally {
      setAddingContact(false)
    }
  }

  // ─── Filter ───────────────────────────────────────────────────
  const filteredGroups = groups.filter(g =>
    g.groupName?.toLowerCase().includes(search.toLowerCase()))

  const filteredUsers = allUsers.filter(u =>
    (u.displayName?.toLowerCase().includes(search.toLowerCase()) ||
     u.phoneNumber?.toLowerCase().includes(search.toLowerCase())))

  return (
    <aside className="sidebar">
      {/* Header */}
      <div className="sidebar-header">
        <Link to="/chat" className="sidebar-brand" style={{ textDecoration: 'none' }}>
          <div className="sidebar-logo">💬</div>
          <h2>WebChat</h2>
        </Link>
        <div className="sidebar-actions">
          {activeTab === 'Groups' && (
            <button className="icon-btn" title="New group" onClick={() => setShowCreate(v => !v)}>
              <svg viewBox="0 0 24 24" width="18" height="18" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
                <path d="M11 4H4a2 2 0 0 0-2 2v14a2 2 0 0 0 2 2h14a2 2 0 0 0 2-2v-7" />
                <path d="M18.5 2.5a2.121 2.121 0 1 1 3 3L12 15l-4 1 1-4z" />
              </svg>
            </button>
          )}
          {activeTab === 'People' && (
            <button className="icon-btn" id="add-contact-btn" title="Add contact" onClick={() => setShowAddContact(v => !v)}>
              <svg viewBox="0 0 24 24" width="18" height="18" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
                <line x1="12" y1="5" x2="12" y2="19" />
                <line x1="5" y1="12" x2="19" y2="12" />
              </svg>
            </button>
          )}
        </div>
      </div>

      {/* New-group inline form */}
      {showCreate && activeTab === 'Groups' && (
        <form onSubmit={handleCreateGroup} style={{
          padding: '10px 14px', borderBottom: '1px solid var(--border)',
          display: 'flex', gap: '8px',
        }}>
          <input
            id="new-group-name"
            type="text"
            placeholder="Group name…"
            value={newGroupName}
            onChange={e => setNewGroupName(e.target.value)}
            autoFocus
            style={{
              flex: 1, padding: '8px 12px',
              background: 'var(--bg-input)',
              border: '1.5px solid var(--border)',
              borderRadius: 'var(--radius-md)',
              color: 'var(--text-primary)', fontSize: '0.87rem',
            }}
          />
          <button
            type="submit"
            disabled={!newGroupName.trim() || creating || !isConnected}
            style={{
              padding: '8px 14px', background: 'var(--accent)', color: '#fff',
              borderRadius: 'var(--radius-sm)', fontSize: '0.82rem', fontWeight: 600,
            }}
          >
            {creating ? '…' : 'Create'}
          </button>
        </form>
      )}

      {/* Add contact inline form */}
      {showAddContact && activeTab === 'People' && (
        <form onSubmit={handleAddContact} style={{
          padding: '10px 14px', borderBottom: '1px solid var(--border)',
          display: 'flex', flexDirection: 'column', gap: '6px',
        }}>
          <div style={{ display: 'flex', gap: '8px' }}>
            <input
              id="new-contact-phone"
              type="text"
              placeholder="Phone number…"
              value={newContactPhone}
              onChange={e => {
                setNewContactPhone(e.target.value)
                setContactError('')
              }}
              autoFocus
              style={{
                flex: 1, padding: '8px 12px',
                background: 'var(--bg-input)',
                border: '1.5px solid var(--border)',
                borderRadius: 'var(--radius-md)',
                color: 'var(--text-primary)', fontSize: '0.87rem',
              }}
            />
            <button
              type="submit"
              id="submit-add-contact-btn"
              disabled={!newContactPhone.trim() || addingContact || !isConnected}
              style={{
                padding: '8px 14px', background: 'var(--accent)', color: '#fff',
                borderRadius: 'var(--radius-sm)', fontSize: '0.82rem', fontWeight: 600,
              }}
            >
              {addingContact ? '…' : 'Add'}
            </button>
          </div>
          {contactError && (
            <span id="add-contact-error" style={{ color: 'var(--danger)', fontSize: '0.78rem', paddingLeft: '4px' }}>
              ⚠️ {contactError}
            </span>
          )}
        </form>
      )}

      {/* Search */}
      <SidebarSearch
        value={search}
        onChange={setSearch}
        placeholder={activeTab === 'Groups' ? 'Search groups…' : 'Search people…'}
      />

      {/* Tabs: Groups | People */}
      <div className="sidebar-tabs">
        {TABS.map(tab => {
          const unreadGroupsCount = groups.reduce((acc, g) => acc + (unreadCounts[g.groupId] || 0), 0)
          const unreadPeopleCount = allUsers.reduce((acc, u) => acc + (unreadCounts[u.phoneNumber] || 0), 0)
          const count = tab === 'Groups' ? unreadGroupsCount : unreadPeopleCount

          return (
            <button
              key={tab}
              className={`tab-btn ${activeTab === tab ? 'active' : ''}`}
              onClick={() => { setActiveTab(tab); setSearch('') }}
            >
              {tab}
              {count > 0 && (
                <span className="unread-badge" style={{
                  marginLeft: 6, background: 'var(--accent)', color: '#fff',
                  borderRadius: 'var(--radius-full)', fontSize: '0.65rem',
                  fontWeight: 700, padding: '2px 7px',
                }}>
                  {count}
                </span>
              )}
            </button>
          )
        })}
      </div>

      {/* List */}
      <div className="contact-list">
        {!isConnected ? (
          <div style={{ padding: '24px 16px', textAlign: 'center', color: 'var(--warning)', fontSize: '0.83rem' }}>
            ⚡ Connecting to server…
          </div>
        ) : activeTab === 'Groups' ? (
          <GroupList
            groups={filteredGroups}
            activeGroupId={activeGroupId}
            userPhone={userPhone}
            onSelect={onSelectGroup}
            onRemove={onRemoveGroup}
            unreadCounts={unreadCounts}
          />
        ) : (
          <OnlineUsers
            users={filteredUsers}
            myPhone={userPhone}
            onlinePhoneNumbers={onlineUsers}
            activeDmPhone={activeDmPhone}
            onSelectUser={onSelectUser}
            onRemoveDm={onRemoveDm}
            unreadCounts={unreadCounts}
          />
        )}
      </div>

      <SidebarFooter
        displayName={displayName}
        isConnected={isConnected}
        onLogout={onLogout}
      />
    </aside>
  )
}

export default Sidebar

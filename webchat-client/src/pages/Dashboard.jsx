import React, { useState, useEffect, useCallback, useRef } from 'react'
import { useNavigate, useParams } from 'react-router-dom'
import { authService } from '../services/authService'
import ws from '../services/websocketService'
import { chatService } from '../services/chatService'
import useWebSocket from '../hooks/useWebSocket'
import Sidebar   from '../components/Sidebar'
import ChatPanel from '../components/ChatPanel'
import Avatar    from '../components/common/Avatar'
import { WS_MESSAGE_TYPES, STORAGE_KEYS } from '../config/constants'
import ConfirmationDialog from '../components/common/ConfirmationDialog'
import '../styles/Dashboard.css'

/**
 * Dashboard — main application shell.
 *
 * Owns:
 * - WebSocket lifecycle (via useWebSocket hook)
 * - Group and user list state
 * - All WS event handlers (stored in refs for stable cleanup)
 * - Navigation between groups and DMs
 */
function Dashboard({ setIsAuthenticated }) {
  const navigate = useNavigate()
  const { groupId, recipientPhone } = useParams()

  const displayName = sessionStorage.getItem(STORAGE_KEYS.DISPLAY_NAME) ?? 'You'
  const userPhone   = sessionStorage.getItem(STORAGE_KEYS.USER_PHONE)   ?? ''

  // Determines chat mode from current URL params
  const chatMode = recipientPhone ? 'dm' : groupId ? 'group' : null

  // ── WebSocket connection (managed by hook) ──────────────────────────────
  const isConnected = useWebSocket(userPhone)

  // ── State ───────────────────────────────────────────────────────────────
  const [activeGroup,    setActiveGroup]    = useState(null)
  const [allGroups,      setAllGroups]      = useState([])
  const [allUsers,       setAllUsers]       = useState([])
  const [onlineUsers,    setOnlineUsers]    = useState([])
  const [groupMembers,   setGroupMembers]   = useState([])
  const [newMemberPhone, setNewMemberPhone] = useState('')
  const [addMemberError, setAddMemberError] = useState('')
  const [unreadCounts, setUnreadCounts] = useState({})
  const [showInfo, setShowInfo] = useState(true)

  // ── Dialog States & Helpers ─────────────────────────────────────────────
  const [dialogState, setDialogState] = useState({
    isOpen: false,
    title: '',
    message: '',
    type: 'info',
    confirmLabel: 'Confirm',
    cancelLabel: 'Cancel',
    isAlert: false,
    resolve: null,
  })

  // ── Data loaders ────────────────────────────────────────────────────────
  const loadGroups = useCallback(async () => {
    try {
      const res  = await chatService.listGroups()
      const list = Array.isArray(res.data) ? res.data : []
      setAllGroups(list)
      return list
    } catch {
      return []
    }
  }, [])

  const loadUsers = useCallback(async () => {
    try {
      const res = await chatService.getAllUsers()
      setAllUsers(Array.isArray(res) ? res : [])
    } catch { /* silent */ }
  }, [])

  const loadGroupMembers = useCallback(async () => {
    if (groupId) {
      try {
        const res = await chatService.listUsers(groupId)
        setGroupMembers(Array.isArray(res.data) ? res.data : [])
      } catch { /* silent */ }
    } else {
      setGroupMembers([])
    }
  }, [groupId])

  // ── Name resolver ───────────────────────────────────────────────────────
  const resolveName = useCallback(
    (phone) => {
      if (phone === userPhone) return 'You'
      const found = allUsers.find((u) => u.phoneNumber === phone)
      return found ? found.displayName : phone
    },
    [allUsers, userPhone]
  )

  const confirmAction = useCallback(({ title, message, confirmLabel, cancelLabel, type }) => {
    return new Promise((resolve) => {
      setDialogState({
        isOpen: true,
        title,
        message,
        confirmLabel: confirmLabel || 'Confirm',
        cancelLabel: cancelLabel || 'Cancel',
        type: type || 'danger',
        isAlert: false,
        resolve,
      })
    })
  }, [])

  const alertAction = useCallback((message, title = 'Notification', type = 'info') => {
    return new Promise((resolve) => {
      setDialogState({
        isOpen: true,
        title,
        message,
        confirmLabel: 'OK',
        cancelLabel: 'Cancel',
        type,
        isAlert: true,
        resolve,
      })
    })
  }, [])

  const handleConfirmDialog = useCallback(() => {
    if (dialogState.resolve) {
      dialogState.resolve(true)
    }
    setDialogState((prev) => ({ ...prev, isOpen: false, resolve: null }))
  }, [dialogState])

  const handleCancelDialog = useCallback(() => {
    if (dialogState.resolve) {
      dialogState.resolve(false)
    }
    setDialogState((prev) => ({ ...prev, isOpen: false, resolve: null }))
  }, [dialogState])

  // ── WS event handlers — stored in refs so cleanup always removes the same fn
  //    Using refs avoids stale closure issues while keeping stable references ─
  const groupIdRef       = useRef(groupId)
  const recipientRef     = useRef(recipientPhone)
  const navigateRef      = useRef(navigate)
  const loadGroupsRef    = useRef(loadGroups)
  const loadGroupMembersRef = useRef(loadGroupMembers)
  const alertActionRef   = useRef(alertAction)

  useEffect(() => { groupIdRef.current       = groupId },       [groupId])
  useEffect(() => { recipientRef.current     = recipientPhone },[recipientPhone])
  useEffect(() => { navigateRef.current      = navigate },      [navigate])
  useEffect(() => { loadGroupsRef.current    = loadGroups },    [loadGroups])
  useEffect(() => { loadGroupMembersRef.current = loadGroupMembers }, [loadGroupMembers])
  useEffect(() => { alertActionRef.current   = alertAction },   [alertAction])

  // ── Stable listener callbacks (created once, reference refs for current values)
  const onConnect = useCallback(() => {
    loadGroupsRef.current()
    loadUsers()
    loadGroupMembersRef.current()
  }, [loadUsers])

  const onCreateGroup = useCallback(() => {
    loadGroupsRef.current()
  }, [])

  const onOnlineUsers = useCallback((msg) => {
    setOnlineUsers(Array.isArray(msg.data) ? msg.data : [])
  }, [])

  const onDeleteGroup = useCallback((msg) => {
    loadGroupsRef.current()
    if (groupIdRef.current && groupIdRef.current === msg.groupId) {
      navigateRef.current('/chat')
      alertActionRef.current(msg.content || 'Group was deleted by the creator.', 'Group Deleted', 'warning')
    }
  }, [])

  const onDeleteDm = useCallback((msg) => {
    loadUsers()
    if (recipientRef.current && recipientRef.current === msg.recipientId) {
      navigateRef.current('/chat')
    }
  }, [loadUsers])

  const onGroupInvitation = useCallback(() => {
    loadGroupsRef.current()
  }, [])

  const onGroupKick = useCallback((msg) => {
    loadGroupsRef.current()
    if (groupIdRef.current && groupIdRef.current === msg.groupId) {
      navigateRef.current('/chat')
      alertActionRef.current(`You were removed from the group: ${msg.content ?? ''}`, 'Removed from Group', 'warning')
    }
  }, [])

  const onAddMemberResponse = useCallback((msg) => {
    if (msg.status === 'SUCCESS') {
      loadGroupMembersRef.current()
      setNewMemberPhone('')
      setAddMemberError('')
    } else {
      setAddMemberError(msg.content || 'Error adding member')
    }
  }, [])

  const onRemoveMemberResponse = useCallback((msg) => {
    if (msg.status === 'SUCCESS') {
      loadGroupMembersRef.current()
      loadGroupsRef.current()
    } else {
      alertActionRef.current(msg.content || 'Error removing member', 'Remove Failed', 'danger')
    }
  }, [])

  const onJoinLeave = useCallback(() => {
    loadGroupsRef.current()
    loadGroupMembersRef.current()
  }, [])

  const onIncomingText = useCallback((msg) => {
    const activeGroupRef = groupIdRef.current
    if (msg.sender !== userPhone && msg.groupId !== activeGroupRef) {
      setUnreadCounts((prev) => ({
        ...prev,
        [msg.groupId]: (prev[msg.groupId] || 0) + 1,
      }))
    }
  }, [userPhone])

  const onIncomingDirectMessage = useCallback((msg) => {
    const activeRecipientRef = recipientRef.current
    if (msg.sender !== userPhone && msg.sender !== activeRecipientRef) {
      setUnreadCounts((prev) => ({
        ...prev,
        [msg.sender]: (prev[msg.sender] || 0) + 1,
      }))
    }
  }, [userPhone])

  const onPromoteMember = useCallback(() => {
    loadGroupMembersRef.current()
  }, [])

  const onDemoteMember = useCallback(() => {
    loadGroupMembersRef.current()
  }, [])

  // Clear unread counts when switching chats
  useEffect(() => {
    const activeId = chatMode === 'group' ? groupId : chatMode === 'dm' ? recipientPhone : null
    if (activeId) {
      setUnreadCounts((prev) => {
        if (!prev[activeId]) return prev
        const updated = { ...prev }
        delete updated[activeId]
        return updated
      })
    }
  }, [groupId, recipientPhone, chatMode])

  // ── Register / deregister WS listeners ─────────────────────────────────
  useEffect(() => {
    ws.on(WS_MESSAGE_TYPES.REGISTER,            onConnect)           // pseudo — fires on _connected
    ws.on('_connected',                          onConnect)
    ws.on(WS_MESSAGE_TYPES.CREATE_GROUP,         onCreateGroup)
    ws.on(WS_MESSAGE_TYPES.LIST_ONLINE_USERS,    onOnlineUsers)
    ws.on(WS_MESSAGE_TYPES.DELETE_GROUP,         onDeleteGroup)
    ws.on(WS_MESSAGE_TYPES.DELETE_DM,            onDeleteDm)
    ws.on(WS_MESSAGE_TYPES.GROUP_INVITATION,     onGroupInvitation)
    ws.on(WS_MESSAGE_TYPES.GROUP_KICK,           onGroupKick)
    ws.on(WS_MESSAGE_TYPES.ADD_GROUP_MEMBER,     onAddMemberResponse)
    ws.on(WS_MESSAGE_TYPES.REMOVE_GROUP_MEMBER,  onRemoveMemberResponse)
    ws.on(WS_MESSAGE_TYPES.PROMOTE_MEMBER,       onPromoteMember)
    ws.on(WS_MESSAGE_TYPES.DEMOTE_MEMBER,        onDemoteMember)
    ws.on(WS_MESSAGE_TYPES.TEXT,                 onIncomingText)
    ws.on(WS_MESSAGE_TYPES.DIRECT_MESSAGE,       onIncomingDirectMessage)
    ws.on(WS_MESSAGE_TYPES.JOIN,                 onJoinLeave)
    ws.on(WS_MESSAGE_TYPES.LEAVE,                onJoinLeave)

    if (isConnected) {
      loadGroups()
      loadUsers()
      loadGroupMembers()
    }

    return () => {
      ws.off(WS_MESSAGE_TYPES.REGISTER,            onConnect)
      ws.off('_connected',                          onConnect)
      ws.off(WS_MESSAGE_TYPES.CREATE_GROUP,         onCreateGroup)
      ws.off(WS_MESSAGE_TYPES.LIST_ONLINE_USERS,    onOnlineUsers)
      ws.off(WS_MESSAGE_TYPES.DELETE_GROUP,         onDeleteGroup)
      ws.off(WS_MESSAGE_TYPES.DELETE_DM,            onDeleteDm)
      ws.off(WS_MESSAGE_TYPES.GROUP_INVITATION,     onGroupInvitation)
      ws.off(WS_MESSAGE_TYPES.GROUP_KICK,           onGroupKick)
      ws.off(WS_MESSAGE_TYPES.ADD_GROUP_MEMBER,     onAddMemberResponse)
      ws.off(WS_MESSAGE_TYPES.REMOVE_GROUP_MEMBER,  onRemoveMemberResponse)
      ws.off(WS_MESSAGE_TYPES.PROMOTE_MEMBER,       onPromoteMember)
      ws.off(WS_MESSAGE_TYPES.DEMOTE_MEMBER,        onDemoteMember)
      ws.off(WS_MESSAGE_TYPES.TEXT,                 onIncomingText)
      ws.off(WS_MESSAGE_TYPES.DIRECT_MESSAGE,       onIncomingDirectMessage)
      ws.off(WS_MESSAGE_TYPES.JOIN,                 onJoinLeave)
      ws.off(WS_MESSAGE_TYPES.LEAVE,                onJoinLeave)
    }
  }, [
    isConnected,
    onConnect, onCreateGroup, onOnlineUsers, onDeleteGroup, onDeleteDm,
    onGroupInvitation, onGroupKick, onAddMemberResponse, onRemoveMemberResponse,
    onPromoteMember, onDemoteMember, onIncomingText, onIncomingDirectMessage, onJoinLeave,
    loadGroups, loadUsers, loadGroupMembers,
  ])

  // ── Resolve active group from allGroups ─────────────────────────────────
  useEffect(() => {
    if (!groupId) { setActiveGroup(null); return }
    const found = allGroups.find((g) => g.groupId === groupId)
    if (found) {
      setActiveGroup(found)
    } else if (isConnected) {
      loadGroups().then((list) => {
        const g = list.find((g) => g.groupId === groupId)
        if (g) setActiveGroup(g)
        else navigate('/chat', { replace: true })
      })
    }
  }, [groupId, allGroups, isConnected, loadGroups, navigate])

  // ── Navigation handlers ──────────────────────────────────────────────────
  const handleSelectGroup = useCallback((group) => navigate(`/chat/group/${group.groupId}`), [navigate])
  const handleSelectUser  = useCallback((phone) => navigate(`/chat/dm/${phone}`), [navigate])

  const handleLogout = useCallback(() => {
    if (activeGroup) ws.send(WS_MESSAGE_TYPES.LEAVE, { groupId: activeGroup.groupId })
    ws.disconnect()
    authService.logout()
    setIsAuthenticated(false)
    navigate('/login')
  }, [activeGroup, navigate, setIsAuthenticated])

  // ── Group action handlers ────────────────────────────────────────────────
  const handleAddMemberSubmit = useCallback(
    (e) => {
      e.preventDefault()
      const phone = newMemberPhone.trim()
      if (!phone || !groupId) return
      ws.send(WS_MESSAGE_TYPES.ADD_GROUP_MEMBER, { groupId, content: phone })
    },
    [newMemberPhone, groupId]
  )

  const handleRemoveMember = useCallback(
    async (phone) => {
      if (!groupId) return
      const confirmed = await confirmAction({
        title: 'Remove Member',
        message: `Remove ${resolveName(phone)} from the group?`,
        confirmLabel: 'Remove Member',
        type: 'danger'
      })
      if (confirmed) {
        ws.send(WS_MESSAGE_TYPES.REMOVE_GROUP_MEMBER, { groupId, content: phone })
      }
    },
    [groupId, resolveName, confirmAction]
  )

  const handlePromoteAdmin = useCallback((phone) => {
    if (!groupId) return
    ws.send(WS_MESSAGE_TYPES.PROMOTE_MEMBER, { groupId, content: phone })
  }, [groupId])

  const handleDemoteAdmin = useCallback((phone) => {
    if (!groupId) return
    ws.send(WS_MESSAGE_TYPES.DEMOTE_MEMBER, { groupId, content: phone })
  }, [groupId])

  const handleDeleteGroup = useCallback(async () => {
    if (!groupId) return
    const confirmed = await confirmAction({
      title: 'Delete Group',
      message: 'Delete this group? All history will be permanently wiped for all members.',
      confirmLabel: 'Delete Group',
      type: 'danger'
    })
    if (confirmed) {
      ws.send(WS_MESSAGE_TYPES.DELETE_GROUP, { groupId })
    }
  }, [groupId, confirmAction])

  const handleLeaveGroup = useCallback(async () => {
    if (!groupId) return
    const confirmed = await confirmAction({
      title: 'Leave Group',
      message: 'Are you sure you want to leave this group?',
      confirmLabel: 'Leave',
      type: 'warning'
    })
    if (confirmed) {
      ws.send(WS_MESSAGE_TYPES.REMOVE_GROUP_MEMBER, { groupId, content: userPhone })
      navigate('/chat')
    }
  }, [groupId, userPhone, navigate, confirmAction])

  const handleDeleteDm = useCallback(async () => {
    if (!recipientPhone) return
    const confirmed = await confirmAction({
      title: 'Delete Chat',
      message: `Delete chat with ${resolveName(recipientPhone)}? All history will be permanently wiped.`,
      confirmLabel: 'Delete Chat',
      type: 'danger'
    })
    if (confirmed) {
      ws.send(WS_MESSAGE_TYPES.DELETE_DM, { recipientId: recipientPhone })
    }
  }, [recipientPhone, resolveName, confirmAction])

  /**
   * Sidebar X button on a group row:
   *   - Creator → delete the entire group (all members lose it)
   *   - Member  → leave the group (others keep chatting)
   */
  const handleSidebarRemoveGroup = useCallback(async (group) => {
    if (!group) return
    const isCreator = group.creator === userPhone
    if (isCreator) {
      const confirmed = await confirmAction({
        title: 'Delete Group',
        message: `Delete "${group.groupName}"? This will permanently remove the group and all history for everyone.`,
        confirmLabel: 'Delete',
        type: 'danger'
      })
      if (confirmed) {
        ws.send(WS_MESSAGE_TYPES.DELETE_GROUP, { groupId: group.groupId })
        if (groupId === group.groupId) navigate('/chat')
      }
    } else {
      const confirmed = await confirmAction({
        title: 'Leave Group',
        message: `Leave "${group.groupName}"? You can be re-added by the group creator.`,
        confirmLabel: 'Leave',
        type: 'warning'
      })
      if (confirmed) {
        ws.send(WS_MESSAGE_TYPES.REMOVE_GROUP_MEMBER, { groupId: group.groupId, content: userPhone })
        if (groupId === group.groupId) navigate('/chat')
      }
    }
  }, [userPhone, groupId, navigate, confirmAction])

  /**
   * Sidebar X button on a contact/DM row:
   * Removes the contact and deletes all DM history between the two users.
   */
  const handleSidebarRemoveDm = useCallback(async (phone) => {
    const name = resolveName(phone)
    const confirmed = await confirmAction({
      title: 'Remove Contact & History',
      message: `Remove "${name}" and delete all chat history? This cannot be undone.`,
      confirmLabel: 'Remove & Delete',
      type: 'danger'
    })
    if (confirmed) {
      ws.send(WS_MESSAGE_TYPES.DELETE_DM, { recipientId: phone })
      if (recipientPhone === phone) navigate('/chat')
    }
  }, [resolveName, recipientPhone, navigate, confirmAction])

  const mediaItems = ['🖼️', '📄', '🎵', '📹', '🖼️', '📄']

  return (
    <div className="dashboard">
      <Sidebar
        activeGroupId={chatMode === 'group' ? groupId : null}
        activeDmPhone={chatMode === 'dm'    ? recipientPhone : null}
        onSelectGroup={handleSelectGroup}
        onSelectUser={handleSelectUser}
        displayName={displayName}
        userPhone={userPhone}
        isConnected={isConnected}
        onLogout={handleLogout}
        groups={allGroups}
        loadGroups={loadGroups}
        allUsers={allUsers}
        loadUsers={loadUsers}
        onlineUsers={onlineUsers}
        onRemoveGroup={handleSidebarRemoveGroup}
        onRemoveDm={handleSidebarRemoveDm}
        unreadCounts={unreadCounts}
      />

      <main className="chat-panel">
        <ChatPanel
          chatMode={chatMode}
          group={chatMode === 'group' ? activeGroup : null}
          dmPhone={chatMode === 'dm' ? recipientPhone : null}
          myPhone={userPhone}
          allUsers={allUsers}
          resolveName={resolveName}
          showInfo={showInfo}
          onToggleInfo={() => setShowInfo(v => !v)}
        />
      </main>

      {/* Group Info Panel */}
      {showInfo && chatMode === 'group' && activeGroup && (
        <aside className="info-panel">
          <div className="info-panel-header">
            <span>Group Info</span>
            <button 
              className="icon-btn" 
              onClick={() => setShowInfo(false)} 
              title="Close panel"
              style={{ width: '28px', height: '28px', borderRadius: 'var(--radius-sm)' }}
            >
              <svg viewBox="0 0 24 24" width="14" height="14" fill="none" stroke="currentColor" strokeWidth="2.5" strokeLinecap="round" strokeLinejoin="round">
                <line x1="18" y1="6" x2="6" y2="18"></line>
                <line x1="6" y1="6" x2="18" y2="18"></line>
              </svg>
            </button>
          </div>
          <div className="info-contact-card">
            <div className="info-avatar">
              {activeGroup.groupName?.charAt(0)?.toUpperCase() ?? 'G'}
            </div>
            <h3>{activeGroup.groupName}</h3>
            <p>Created by {resolveName(activeGroup.creator)}</p>
            <p style={{ color: 'var(--success)', fontSize: '0.8rem' }}>
              👥 {groupMembers.length} member{groupMembers.length !== 1 ? 's' : ''}
            </p>
          </div>

           {/* Members section */}
          <div className="info-section">
            <h4>Members</h4>
            <div className="member-list" style={{ display: 'flex', flexDirection: 'column', gap: '8px', maxHeight: '200px', overflowY: 'auto', paddingRight: '4px' }}>
              {(() => {
                const myMemberDetail = groupMembers.find(member => member.phoneNumber === userPhone);
                const isMeAdmin = myMemberDetail ? (myMemberDetail.isAdmin === 'true' || myMemberDetail.isAdmin === true) : false;
                const isMeCreator = activeGroup.creator === userPhone;

                return groupMembers.map((m) => {
                  const mIsAdmin = m.isAdmin === 'true' || m.isAdmin === true;
                  const mIsCreator = m.isCreator === 'true' || m.isCreator === true;
                  const canRemove = m.phoneNumber !== userPhone && (
                    isMeCreator || (isMeAdmin && !mIsAdmin)
                  );

                  return (
                    <div key={m.phoneNumber} className="member-item" style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', gap: '8px' }}>
                      <div style={{ display: 'flex', alignItems: 'center', gap: '8px', minWidth: 0 }}>
                        <Avatar name={m.displayName} size={28} />
                        <div style={{ minWidth: 0 }}>
                          <div className="member-name" style={{ fontSize: '0.85rem', fontWeight: 600, color: 'var(--text-primary)', whiteSpace: 'nowrap', overflow: 'hidden', textOverflow: 'ellipsis' }}>
                            {m.displayName}
                          </div>
                          <div className="member-phone" style={{ fontSize: '0.7rem', color: 'var(--text-secondary)' }}>
                            {m.phoneNumber}
                          </div>
                        </div>
                      </div>
                      <div style={{ display: 'flex', alignItems: 'center', gap: '6px' }}>
                        {mIsCreator ? (
                          <span className="role-badge" style={{ fontSize: '0.62rem', background: 'rgba(108,99,255,0.15)', color: 'var(--accent)', padding: '2px 6px', borderRadius: '4px', fontWeight: 700 }} title="Group Owner">
                            Owner
                          </span>
                        ) : mIsAdmin ? (
                          <span className="role-badge" style={{ fontSize: '0.62rem', background: 'rgba(0,212,170,0.15)', color: 'var(--teal)', padding: '2px 6px', borderRadius: '4px', fontWeight: 700 }} title="Group Admin">
                            Admin
                          </span>
                        ) : null}

                        {/* Owner Admin Management Controls */}
                        {isMeCreator && m.phoneNumber !== userPhone && (
                          mIsAdmin ? (
                            <button
                              type="button"
                              onClick={() => handleDemoteAdmin(m.phoneNumber)}
                              title="Demote to normal member"
                              style={{ background: 'rgba(246,169,75,0.1)', color: 'var(--warning)', fontSize: '0.65rem', fontWeight: 700, padding: '2px 6px', borderRadius: '4px' }}
                            >
                              Demote
                            </button>
                          ) : (
                            <button
                              type="button"
                              onClick={() => handlePromoteAdmin(m.phoneNumber)}
                              title="Promote to admin"
                              style={{ background: 'rgba(0,212,170,0.1)', color: 'var(--teal)', fontSize: '0.65rem', fontWeight: 700, padding: '2px 6px', borderRadius: '4px' }}
                            >
                              Promote
                            </button>
                          )
                        )}

                        {/* Member Removal Controls (Admins and Owner only) */}
                        {canRemove && (
                          <button
                            type="button"
                            className="remove-member-btn"
                            onClick={() => handleRemoveMember(m.phoneNumber)}
                            title="Remove member"
                            style={{ background: 'transparent', color: 'var(--error)', fontSize: '0.9rem', padding: '2px 6px', borderRadius: '4px' }}
                          >
                            ✕
                          </button>
                        )}
                      </div>
                    </div>
                  );
                });
              })()}
            </div>

            {/* Add member form */}
            <form onSubmit={handleAddMemberSubmit} style={{ marginTop: '14px', display: 'flex', flexDirection: 'column', gap: '6px' }}>
              <div style={{ display: 'flex', gap: '6px' }}>
                <input
                  type="text"
                  placeholder="Member phone number…"
                  value={newMemberPhone}
                  autoComplete="off"
                  onChange={(e) => {
                    setNewMemberPhone(e.target.value)
                    setAddMemberError('')
                  }}
                  style={{
                    flex: 1, padding: '6px 10px',
                    background: 'var(--bg-input)',
                    border: '1.5px solid var(--border)',
                    borderRadius: 'var(--radius-md)',
                    color: 'var(--text-primary)', fontSize: '0.8rem',
                  }}
                />
                <button
                  type="submit"
                  disabled={!newMemberPhone.trim() || !isConnected}
                  style={{
                    padding: '6px 10px', background: 'var(--accent)', color: '#fff',
                    borderRadius: 'var(--radius-sm)', fontSize: '0.78rem', fontWeight: 600,
                  }}
                >
                  Add
                </button>
              </div>
              {addMemberError && (
                <span style={{ color: 'var(--error)', fontSize: '0.73rem', paddingLeft: '2px' }}>
                  ⚠️ {addMemberError}
                </span>
              )}
            </form>
          </div>

          <div className="info-section">
            <h4>Details</h4>
            <div className="info-stat"><span>🆔</span><span style={{ fontSize: '0.72rem', wordBreak: 'break-all' }}>{activeGroup.groupId}</span></div>
            <div className="info-stat"><span>🔒</span><span>Encrypted in transit</span></div>
          </div>

          <div className="info-danger" style={{ marginTop: 'auto', padding: '16px', display: 'flex', flexDirection: 'column', gap: '8px' }}>
            <button className="btn-clear-chat" onClick={handleLeaveGroup} style={{ display: 'flex', alignItems: 'center', justifyContent: 'center', width: '100%', padding: '10px', borderRadius: 'var(--radius-sm)', fontWeight: 600, background: 'rgba(255,255,255,0.05)', color: 'var(--text-primary)' }}>
              🚪 Leave Group
            </button>
            {activeGroup.creator === userPhone && (
              <button className="btn-clear-chat" onClick={handleDeleteGroup} style={{ background: 'var(--error-bg)', color: 'var(--error)', display: 'flex', alignItems: 'center', justifyContent: 'center', width: '100%', padding: '10px', borderRadius: 'var(--radius-sm)', fontWeight: 600 }}>
                🗑️ Delete Group
              </button>
            )}
          </div>
        </aside>
      )}

      {/* DM Info Panel */}
      {showInfo && chatMode === 'dm' && recipientPhone && (
        <aside className="info-panel">
          <div className="info-panel-header">
            <span>Contact Info</span>
            <button 
              className="icon-btn" 
              onClick={() => setShowInfo(false)} 
              title="Close panel"
              style={{ width: '28px', height: '28px', borderRadius: 'var(--radius-sm)' }}
            >
              <svg viewBox="0 0 24 24" width="14" height="14" fill="none" stroke="currentColor" strokeWidth="2.5" strokeLinecap="round" strokeLinejoin="round">
                <line x1="18" y1="6" x2="6" y2="18"></line>
                <line x1="6" y1="6" x2="18" y2="18"></line>
              </svg>
            </button>
          </div>
          <div className="info-contact-card">
            <Avatar name={resolveName(recipientPhone)} size={72} />
            <h3>{resolveName(recipientPhone)}</h3>
            <p>{recipientPhone}</p>
            <span style={{
              fontSize: '0.75rem',
              color: onlineUsers.includes(recipientPhone) ? 'var(--success)' : 'var(--text-secondary)',
              display: 'flex',
              alignItems: 'center',
              gap: '4px',
            }}>
              ● {onlineUsers.includes(recipientPhone) ? 'Online' : 'Offline'}
            </span>
          </div>
          <div className="info-section">
            <h4>Details</h4>
            <div className="info-stat"><span>🔒</span><span>Private conversation</span></div>
            <div className="info-stat"><span>🔔</span><span>Notifications active</span></div>
          </div>
          <div className="info-section">
            <h4>Shared Media</h4>
            <div className="media-grid">
              {mediaItems.map((icon, i) => <div key={i} className="media-thumb">{icon}</div>)}
            </div>
          </div>
          <div className="info-danger" style={{ marginTop: 'auto', padding: '16px' }}>
            <button className="btn-clear-chat" onClick={handleDeleteDm} style={{ background: 'var(--error-bg)', color: 'var(--error)', display: 'flex', alignItems: 'center', justifyContent: 'center', width: '100%', padding: '10px', borderRadius: 'var(--radius-sm)', fontWeight: 600 }}>
              🗑️ Delete Chat
            </button>
          </div>
        </aside>
      )}

      {/* Custom Confirmation / Alert Dialog */}
      <ConfirmationDialog
        isOpen={dialogState.isOpen}
        title={dialogState.title}
        message={dialogState.message}
        confirmLabel={dialogState.confirmLabel}
        cancelLabel={dialogState.cancelLabel}
        type={dialogState.type}
        isAlert={dialogState.isAlert}
        onConfirm={handleConfirmDialog}
        onCancel={handleCancelDialog}
      />
    </div>
  )
}

export default Dashboard

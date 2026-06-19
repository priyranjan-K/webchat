import { useEffect, useState } from 'react'
import ws from '../services/websocketService'

/**
 * useWebSocket — manages WS connection lifecycle.
 * Connects on mount, disconnects on unmount.
 * Returns isConnected status.
 */
function useWebSocket(userPhone) {
  const [isConnected, setIsConnected] = useState(ws.isConnected)

  useEffect(() => {
    if (!userPhone) return

    const onConnect    = () => setIsConnected(true)
    const onDisconnect = () => setIsConnected(false)

    ws.on('_connected',    onConnect)
    ws.on('_disconnected', onDisconnect)

    if (!ws.isConnected) {
      ws.connect(userPhone)
    } else {
      setIsConnected(true)
    }

    return () => {
      ws.off('_connected',    onConnect)
      ws.off('_disconnected', onDisconnect)
    }
  }, [userPhone])

  return isConnected
}

export default useWebSocket

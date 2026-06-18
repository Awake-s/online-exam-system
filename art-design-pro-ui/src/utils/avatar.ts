const AVATAR_COLORS = [
  '#6366f1', '#8b5cf6', '#a855f7', '#d946ef',
  '#ec4899', '#f43f5e', '#ef4444', '#f97316',
  '#f59e0b', '#eab308', '#84cc16', '#22c55e',
  '#14b8a6', '#06b6d4', '#0ea5e9', '#3b82f6'
]

export function generateDefaultAvatar(name?: string | null): string {
  const displayChar = name ? name.charAt(0).toUpperCase() : '?'
  const colorIndex = name
    ? name.split('').reduce((acc, c) => acc + c.charCodeAt(0), 0) % AVATAR_COLORS.length
    : 0
  const bg = AVATAR_COLORS[colorIndex]
  const svg = `<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 100 100">
    <rect width="100" height="100" rx="50" fill="${bg}"/>
    <text x="50" y="54" text-anchor="middle" dominant-baseline="central"
      font-family="system-ui,-apple-system,sans-serif" font-size="44" font-weight="600" fill="white">
      ${displayChar}
    </text>
  </svg>`
  return 'data:image/svg+xml,' + encodeURIComponent(svg)
}

export function getUserAvatar(avatar?: string | null, name?: string | null): string {
  return avatar || generateDefaultAvatar(name)
}

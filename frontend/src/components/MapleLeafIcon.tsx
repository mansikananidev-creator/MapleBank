type MapleLeafIconProps = {
    size?: number
    className?: string
}

// A simplified, stylized maple leaf mark used as the Maple Bank logo.
export default function MapleLeafIcon({ size = 20, className }: MapleLeafIconProps) {
    return (
        <svg
            width={size}
            height={size}
            viewBox="0 0 24 24"
            fill="currentColor"
            className={className}
            aria-hidden="true"
        >
            <path d="M12,2 L14,6.6 L17.5,4.2 L17.1,8.4 L21.7,9.4 L18,12.5 L20,14.9 L16.1,14.9 L14.1,15.6 L12,22 L9.9,15.6 L7.9,14.9 L4,14.9 L6,12.5 L2.3,9.4 L6.9,8.4 L6.5,4.2 L10,6.6 Z" />
        </svg>
    )
}

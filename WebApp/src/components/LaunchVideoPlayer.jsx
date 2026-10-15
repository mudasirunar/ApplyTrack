import React, { useState, useRef, useEffect, useCallback } from 'react';
import './LaunchVideoPlayer.css';

export default function LaunchVideoPlayer() {
  const videoRef = useRef(null);
  const containerRef = useRef(null);
  const [isPlaying, setIsPlaying] = useState(true);
  const [isMuted, setIsMuted] = useState(true);
  const [progress, setProgress] = useState(0);
  const [isFullscreen, setIsFullscreen] = useState(false);
  const [showControls, setShowControls] = useState(true);
  const [currentTime, setCurrentTime] = useState(0);
  const [duration, setDuration] = useState(0);
  const [isBuffering, setIsBuffering] = useState(false);
  const controlsTimeoutRef = useRef(null);
  const userPausedRef = useRef(false);
  const wasPlayingBeforeScrollRef = useRef(true);

  // Format seconds to mm:ss
  const formatTime = (secs) => {
    if (!secs || isNaN(secs) || secs < 0) return '0:00';
    const m = Math.floor(secs / 60);
    const s = Math.floor(secs % 60);
    return `${m}:${s < 10 ? '0' : ''}${s}`;
  };

  // IntersectionObserver: Auto-play when scrolled in, auto-pause when out of view
  useEffect(() => {
    const video = videoRef.current;
    if (!video) return;

    const observer = new IntersectionObserver(
      (entries) => {
        entries.forEach((entry) => {
          if (entry.isIntersecting) {
            // Only auto-play if user did NOT explicitly pause the video
            if (!userPausedRef.current) {
              video.play().then(() => setIsPlaying(true)).catch(() => {});
            }
          } else {
            // When leaving view, pause playback and track whether it was active
            if (!video.paused) {
              wasPlayingBeforeScrollRef.current = true;
              video.pause();
              setIsPlaying(false);
            }
          }
        });
      },
      { threshold: 0.25 }
    );

    if (containerRef.current) {
      observer.observe(containerRef.current);
    }

    return () => {
      observer.disconnect();
    };
  }, []);

  // Sync fullscreen state if user exits via Escape key or browser UI
  useEffect(() => {
    const handleFullscreenChange = () => {
      const isNative = !!(document.fullscreenElement || document.webkitFullscreenElement);
      setIsFullscreen(isNative);
      if (!isNative) {
        document.body.style.overflow = '';
      }
    };
    document.addEventListener('fullscreenchange', handleFullscreenChange);
    document.addEventListener('webkitfullscreenchange', handleFullscreenChange);
    return () => {
      document.removeEventListener('fullscreenchange', handleFullscreenChange);
      document.removeEventListener('webkitfullscreenchange', handleFullscreenChange);
      document.body.style.overflow = '';
    };
  }, []);

  // Listen for Escape key to exit pseudo-fullscreen on iOS / PWA / keyboards
  useEffect(() => {
    const handleKeyDown = (e) => {
      if (e.key === 'Escape' && isFullscreen) {
        if (document.exitFullscreen) {
          document.exitFullscreen().catch(() => {});
        } else if (document.webkitExitFullscreen) {
          document.webkitExitFullscreen();
        }
        setIsFullscreen(false);
        document.body.style.overflow = '';
      }
    };
    window.addEventListener('keydown', handleKeyDown);
    return () => window.removeEventListener('keydown', handleKeyDown);
  }, [isFullscreen]);

  // Auto-pause when phone is locked or screen/tab is hidden
  useEffect(() => {
    const handleVisibilityChange = () => {
      if (document.hidden) {
        const video = videoRef.current;
        if (video && !video.paused) {
          video.pause();
          setIsPlaying(false);
        }
      }
    };
    document.addEventListener('visibilitychange', handleVisibilityChange);
    return () => document.removeEventListener('visibilitychange', handleVisibilityChange);
  }, []);

  // Web Media Session API for Dynamic Island, Lock Screen & Notification Center Controls
  useEffect(() => {
    if (!('mediaSession' in navigator)) return;

    try {
      navigator.mediaSession.metadata = new MediaMetadata({
        title: 'Introducing ApplyTrack',
        artist: 'ApplyTrack Product Tour',
        album: 'Modern Career & Pipeline Suite',
        artwork: [
          { src: '/app_icon.png', sizes: '192x192', type: 'image/png' },
          { src: '/og-preview.png', sizes: '1200x630', type: 'image/png' },
          { src: '/videos/applytrack-poster.jpg', sizes: '1280x720', type: 'image/jpeg' }
        ]
      });

      navigator.mediaSession.setActionHandler('play', () => {
        const video = videoRef.current;
        if (video) {
          userPausedRef.current = false;
          video.play().then(() => setIsPlaying(true)).catch(() => {});
        }
      });

      navigator.mediaSession.setActionHandler('pause', () => {
        const video = videoRef.current;
        if (video) {
          userPausedRef.current = true;
          video.pause();
          setIsPlaying(false);
        }
      });

      navigator.mediaSession.setActionHandler('seekto', (details) => {
        const video = videoRef.current;
        if (video && details.seekTime !== undefined) {
          video.currentTime = details.seekTime;
        }
      });

      navigator.mediaSession.setActionHandler('seekbackward', (details) => {
        const video = videoRef.current;
        if (video) {
          const skip = details.seekOffset || 5;
          video.currentTime = Math.max(video.currentTime - skip, 0);
        }
      });

      navigator.mediaSession.setActionHandler('seekforward', (details) => {
        const video = videoRef.current;
        if (video) {
          const skip = details.seekOffset || 5;
          video.currentTime = Math.min(video.currentTime + skip, video.duration || 0);
        }
      });
    } catch {}

    return () => {
      if ('mediaSession' in navigator) {
        try {
          navigator.mediaSession.metadata = null;
        } catch {}
      }
    };
  }, []);

  // Sync MediaSession playbackState with player state
  useEffect(() => {
    if ('mediaSession' in navigator) {
      navigator.mediaSession.playbackState = isPlaying ? 'playing' : 'paused';
    }
  }, [isPlaying]);

  // Update progress bar & timecode
  const handleTimeUpdate = () => {
    const video = videoRef.current;
    if (video) {
      setCurrentTime(video.currentTime);
      if (video.duration) {
        setDuration(video.duration);
        setProgress((video.currentTime / video.duration) * 100);

        if ('mediaSession' in navigator && 'setPositionState' in navigator.mediaSession) {
          try {
            navigator.mediaSession.setPositionState({
              duration: video.duration,
              playbackRate: video.playbackRate || 1,
              position: video.currentTime
            });
          } catch {}
        }
      }
    }
  };

  // Toggle play/pause (User explicit interaction)
  const togglePlay = useCallback(() => {
    const video = videoRef.current;
    if (!video) return;

    if (video.paused) {
      // User explicitly resumed playback
      userPausedRef.current = false;
      wasPlayingBeforeScrollRef.current = true;
      video.play().then(() => setIsPlaying(true)).catch(() => {});
    } else {
      // User explicitly paused playback: remember choice so it won't auto-play on scroll
      userPausedRef.current = true;
      wasPlayingBeforeScrollRef.current = false;
      video.pause();
      setIsPlaying(false);
    }
  }, []);

  // Toggle audio
  const toggleMute = useCallback((e) => {
    e.stopPropagation();
    const video = videoRef.current;
    if (!video) return;

    video.muted = !video.muted;
    setIsMuted(video.muted);
  }, []);

  // Toggle Fullscreen (Universal Mobile, iOS Safari/Chrome, PWA & Desktop)
  const toggleFullscreen = useCallback((e) => {
    e.stopPropagation();
    const container = containerRef.current;
    if (!container) return;

    const isNativeFullscreen = !!(document.fullscreenElement || document.webkitFullscreenElement);

    if (isFullscreen || isNativeFullscreen) {
      if (document.exitFullscreen) {
        document.exitFullscreen().catch(() => {});
      } else if (document.webkitExitFullscreen) {
        document.webkitExitFullscreen();
      }
      setIsFullscreen(false);
      document.body.style.overflow = '';
    } else {
      let nativeRequested = false;
      try {
        if (container.requestFullscreen) {
          container.requestFullscreen().then(() => {
            setIsFullscreen(true);
          }).catch(() => {
            // iOS Chrome / PWA or permission denial fallback
            setIsFullscreen(true);
            document.body.style.overflow = 'hidden';
          });
          nativeRequested = true;
        } else if (container.webkitRequestFullscreen) {
          container.webkitRequestFullscreen();
          setIsFullscreen(true);
          nativeRequested = true;
        }
      } catch {
        nativeRequested = false;
      }

      if (!nativeRequested) {
        // iOS WebKit & iOS PWA where container.requestFullscreen is unsupported on <div>
        setIsFullscreen(true);
        document.body.style.overflow = 'hidden';
      }
    }
  }, [isFullscreen]);

  // Handle seeking by clicking on the progress bar
  const handleProgressClick = (e) => {
    e.stopPropagation();
    const video = videoRef.current;
    if (!video || !video.duration) return;

    const rect = e.currentTarget.getBoundingClientRect();
    const pos = (e.clientX - rect.left) / rect.width;
    video.currentTime = pos * video.duration;
  };

  // Fade out controls after 2.2s of inactivity (even if hovering stationary), or on mouse leave
  const resetControlsTimer = useCallback(() => {
    setShowControls(true);
    if (controlsTimeoutRef.current) clearTimeout(controlsTimeoutRef.current);
    controlsTimeoutRef.current = setTimeout(() => {
      const video = videoRef.current;
      if (video && !video.paused) {
        setShowControls(false);
      }
    }, 2200);
  }, []);

  const handleMouseLeave = useCallback(() => {
    const video = videoRef.current;
    if (video && !video.paused) {
      if (controlsTimeoutRef.current) clearTimeout(controlsTimeoutRef.current);
      setShowControls(false);
    }
  }, []);

  // When paused, always ensure controls are visible
  useEffect(() => {
    if (!isPlaying) {
      setShowControls(true);
      if (controlsTimeoutRef.current) clearTimeout(controlsTimeoutRef.current);
    } else {
      resetControlsTimer();
    }
  }, [isPlaying, resetControlsTimer]);

  useEffect(() => {
    return () => {
      if (controlsTimeoutRef.current) clearTimeout(controlsTimeoutRef.current);
    };
  }, []);

  return (
    <div 
      className={`launch-video-wrapper ${!showControls && isPlaying ? 'controls-hidden' : ''} ${isFullscreen ? 'is-fullscreen-mode' : ''}`} 
      ref={containerRef}
      onMouseMove={resetControlsTimer}
      onMouseEnter={resetControlsTimer}
      onMouseLeave={handleMouseLeave}
      onTouchStart={resetControlsTimer}
      onClick={togglePlay}
    >
      <video
        ref={videoRef}
        className="launch-video-element"
        poster="/videos/applytrack-poster.jpg"
        playsInline
        muted
        loop
        preload="metadata"
        onTimeUpdate={handleTimeUpdate}
        onLoadedMetadata={(e) => {
          if (e.currentTarget.duration) setDuration(e.currentTarget.duration);
        }}
        onWaiting={() => setIsBuffering(true)}
        onPlaying={() => setIsBuffering(false)}
        onCanPlay={() => setIsBuffering(false)}
        onSeeking={() => setIsBuffering(true)}
        onSeeked={() => setIsBuffering(false)}
        aria-label="ApplyTrack Product Tour Video"
      >
        <source src="/videos/applytrack-launch.webm" type="video/webm" />
        <source src="/videos/applytrack-launch.mp4" type="video/mp4" />
        Your browser does not support HTML5 video.
      </video>

      {/* Floating Center Buffering Spinner or Play Badge */}
      {isBuffering ? (
        <div className="launch-video-center-spinner" aria-label="Buffering video">
          <div className="video-spinner-ring" />
        </div>
      ) : !isPlaying ? (
        <div className="launch-video-center-play">
          <div className="play-pulse-circle">
            <svg viewBox="0 0 24 24" width="32" height="32" fill="currentColor">
              <path d="M8 5v14l11-7z" />
            </svg>
          </div>
        </div>
      ) : null}

      {/* Overlay Control Bar */}
      <div className={`launch-video-controls ${showControls || !isPlaying ? 'controls-visible' : ''}`}>
        {/* Play/Pause Button */}
        <button 
          type="button"
          className="video-ctrl-btn"
          onClick={(e) => { e.stopPropagation(); togglePlay(); }}
          title={isPlaying ? "Pause" : "Play"}
          aria-label={isPlaying ? "Pause Video" : "Play Video"}
        >
          {isPlaying ? (
            <svg viewBox="0 0 24 24" width="20" height="20" fill="currentColor">
              <path d="M6 19h4V5H6v14zm8-14v14h4V5h-4z" />
            </svg>
          ) : (
            <svg viewBox="0 0 24 24" width="20" height="20" fill="currentColor">
              <path d="M8 5v14l11-7z" />
            </svg>
          )}
        </button>

        {/* Audio Mute/Unmute Pill */}
        <button
          type="button"
          className={`video-audio-pill ${!isMuted ? 'audio-active' : ''}`}
          onClick={toggleMute}
          title={isMuted ? "Unmute Sound" : "Mute Sound"}
          aria-label={isMuted ? "Unmute Sound" : "Mute Sound"}
        >
          {isMuted ? (
            <>
              <svg viewBox="0 0 24 24" width="18" height="18" fill="currentColor">
                <path d="M16.5 12c0-1.77-1.02-3.29-2.5-4.03v2.21l2.45 2.45c.03-.2.05-.41.05-.63zm2.5 0c0 .94-.2 1.82-.54 2.64l1.51 1.51C20.63 14.91 21 13.5 21 12c0-4.28-2.99-7.86-7-8.77v2.06c2.89.86 5 3.54 5 6.71zM4.27 3L3 4.27 7.73 9H3v6h4l5 5v-6.73l4.25 4.25c-.67.52-1.42.93-2.25 1.18v2.06c1.38-.31 2.63-.95 3.69-1.81L19.73 21 21 19.73l-9-9L4.27 3zM12 4L9.91 6.09 12 8.18V4z" />
              </svg>
              <span>Unmute Sound</span>
            </>
          ) : (
            <>
              <svg viewBox="0 0 24 24" width="18" height="18" fill="currentColor">
                <path d="M3 9v6h4l5 5V4L7 9H3zm13.5 3c0-1.77-1.02-3.29-2.5-4.03v8.05c1.48-.73 2.5-2.25 2.5-4.02zM14 3.23v2.06c2.89.86 5 3.54 5 6.71s-2.11 5.85-5 6.71v2.06c4.01-.91 7-4.49 7-8.77s-2.99-7.86-7-8.77z" />
              </svg>
              <span>Sound On</span>
            </>
          )}
        </button>

        {/* Timeline Progress Bar */}
        <div 
          className="video-progress-track"
          onClick={handleProgressClick}
          title="Scrub timeline"
        >
          <div className="video-progress-fill" style={{ width: `${progress}%` }} />
        </div>

        {/* Real Dynamic Timestamp */}
        <span className="video-badge-duration">
          {formatTime(currentTime)} / {formatTime(duration)}
        </span>

        {/* Fullscreen Button */}
        <button
          type="button"
          className="video-ctrl-btn"
          onClick={toggleFullscreen}
          title="Fullscreen"
          aria-label="Toggle Fullscreen"
        >
          {isFullscreen ? (
            <svg viewBox="0 0 24 24" width="18" height="18" fill="currentColor">
              <path d="M5 16h3v3h2v-5H5v2zm3-8H5v2h5V5H8v3zm6 11h2v-3h3v-2h-5v5zm2-11V5h-2v5h5V8h-3z" />
            </svg>
          ) : (
            <svg viewBox="0 0 24 24" width="18" height="18" fill="currentColor">
              <path d="M7 14H5v5h5v-2H7v-3zm-2-4h2V7h3V5H5v5zm12 7h-3v2h5v-5h-2v3zM14 5v2h3v3h2V5h-5z" />
            </svg>
          )}
        </button>
      </div>
    </div>
  );
}

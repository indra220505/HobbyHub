-- 1. USERS TABLE
CREATE TABLE IF NOT EXISTS users (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    username VARCHAR(32) UNIQUE NOT NULL,
    email VARCHAR(255) UNIQUE NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    display_name VARCHAR(64) NOT NULL,
    avatar_url TEXT,
    banner_url TEXT,
    bio TEXT,
    reputation_score INT DEFAULT 0,
    level INT DEFAULT 1,
    current_xp BIGINT DEFAULT 0,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

-- 2. CATEGORIES TABLE
CREATE TABLE IF NOT EXISTS categories (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name VARCHAR(64) NOT NULL,
    slug VARCHAR(64) UNIQUE NOT NULL,
    icon_url TEXT
);

-- 3. COMMUNITIES TABLE
CREATE TABLE IF NOT EXISTS communities (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    category_id UUID REFERENCES categories(id) ON DELETE SET NULL,
    creator_id UUID NOT NULL REFERENCES users(id),
    name VARCHAR(64) NOT NULL,
    slug VARCHAR(64) UNIQUE NOT NULL,
    description TEXT,
    icon_url TEXT,
    member_count INT DEFAULT 1,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

-- 4. CHANNELS TABLE
CREATE TABLE IF NOT EXISTS channels (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    community_id UUID NOT NULL REFERENCES communities(id) ON DELETE CASCADE,
    name VARCHAR(64) NOT NULL,
    type VARCHAR(32) NOT NULL DEFAULT 'TEXT_CHAT',
    topic TEXT,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

-- 5. SEED INITIAL CATEGORIES
INSERT INTO categories (id, name, slug, icon_url) VALUES 
('c0000000-0000-0000-0000-000000000001', 'Programming & Tech', 'programming', 'code_icon'),
('c0000000-0000-0000-0000-000000000002', 'Gaming & Esports', 'gaming', 'gamepad_icon'),
('c0000000-0000-0000-0000-000000000003', 'Creative & Art', 'art', 'palette_icon')
ON CONFLICT (slug) DO NOTHING;

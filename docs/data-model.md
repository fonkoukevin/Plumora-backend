# Plumora Data Model — MVP

## Tables

### users
- id_user UUID PK
- firstname VARCHAR(80) NOT NULL
- lastname VARCHAR(80) NOT NULL
- username VARCHAR(50) UNIQUE NOT NULL
- email VARCHAR(150) UNIQUE NOT NULL
- password_hash VARCHAR(255) NOT NULL
- avatar_url VARCHAR(500)
- bio TEXT
- is_active BOOLEAN DEFAULT TRUE
- created_at TIMESTAMP NOT NULL
- updated_at TIMESTAMP

### roles
- id_role UUID PK
- name VARCHAR(30) UNIQUE NOT NULL
- description VARCHAR(255)

Role values:
- AUTHOR
- READER
- BETA_READER
- ADMIN

### user_roles
- id_user UUID FK users(id_user)
- id_role UUID FK roles(id_role)
- assigned_at TIMESTAMP NOT NULL
- PK(id_user, id_role)

### books
- id_book UUID PK
- author_id UUID FK users(id_user)
- title VARCHAR(150) NOT NULL
- subtitle VARCHAR(200)
- summary TEXT
- cover_url VARCHAR(500)
- genre VARCHAR(80) NOT NULL
- language_code VARCHAR(10) DEFAULT 'fr'
- status VARCHAR(40) NOT NULL
- visibility VARCHAR(40) NOT NULL
- published_at TIMESTAMP
- reading_count INTEGER DEFAULT 0
- average_rating DECIMAL(3,2) DEFAULT 0.00
- created_at TIMESTAMP NOT NULL
- updated_at TIMESTAMP

BookStatus:
- DRAFT
- IN_BETA_READING
- IN_CORRECTION
- READY_TO_PUBLISH
- PUBLISHED
- ARCHIVED

BookVisibility:
- PRIVATE
- BETA_ONLY
- PUBLIC

### chapters
- id_chapter UUID PK
- book_id UUID FK books(id_book)
- title VARCHAR(150) NOT NULL
- content TEXT
- chapter_order INTEGER NOT NULL
- word_count INTEGER DEFAULT 0
- created_at TIMESTAMP NOT NULL
- updated_at TIMESTAMP
- UNIQUE(book_id, chapter_order)

### chapter_versions
- id_chapter_version UUID PK
- chapter_id UUID FK chapters(id_chapter)
- created_by_user_id UUID FK users(id_user)
- version_number INTEGER NOT NULL
- content_snapshot TEXT NOT NULL
- created_at TIMESTAMP NOT NULL
- UNIQUE(chapter_id, version_number)

### ai_writing_requests
- id_ai_writing_request UUID PK
- user_id UUID FK users(id_user)
- chapter_id UUID FK chapters(id_chapter)
- selected_text TEXT NOT NULL
- context_text TEXT
- action_type VARCHAR(50) NOT NULL
- created_at TIMESTAMP NOT NULL

AiWritingActionType:
- REFORMULATE
- IMPROVE_STYLE
- FIX_REPETITIONS
- MAKE_MORE_EMOTIONAL
- MAKE_DIALOGUE_NATURAL

### ai_writing_suggestions
- id_ai_writing_suggestion UUID PK
- request_id UUID FK ai_writing_requests(id_ai_writing_request)
- suggestion_text TEXT NOT NULL
- explanation TEXT
- status VARCHAR(30) NOT NULL
- created_at TIMESTAMP NOT NULL

AiSuggestionStatus:
- PENDING
- ACCEPTED
- MODIFIED
- IGNORED

### beta_reading_campaigns
- id_beta_reading_campaign UUID PK
- book_id UUID FK books(id_book)
- author_id UUID FK users(id_user)
- title VARCHAR(150) NOT NULL
- instructions TEXT
- deadline DATE
- status VARCHAR(30) NOT NULL
- created_at TIMESTAMP NOT NULL
- closed_at TIMESTAMP

### beta_invitations
- id_beta_invitation UUID PK
- campaign_id UUID FK beta_reading_campaigns(id_beta_reading_campaign)
- beta_reader_id UUID FK users(id_user)
- status VARCHAR(30) NOT NULL
- invited_at TIMESTAMP NOT NULL
- responded_at TIMESTAMP
- UNIQUE(campaign_id, beta_reader_id)

### beta_shared_chapters
- id_beta_shared_chapter UUID PK
- campaign_id UUID FK beta_reading_campaigns(id_beta_reading_campaign)
- chapter_id UUID FK chapters(id_chapter)
- UNIQUE(campaign_id, chapter_id)

### beta_comments
- id_beta_comment UUID PK
- campaign_id UUID FK beta_reading_campaigns(id_beta_reading_campaign)
- chapter_id UUID FK chapters(id_chapter)
- beta_reader_id UUID FK users(id_user)
- comment_text TEXT NOT NULL
- selected_text TEXT
- position_start INTEGER
- position_end INTEGER
- feedback_type VARCHAR(40) NOT NULL
- priority VARCHAR(30) NOT NULL
- status VARCHAR(30) NOT NULL
- created_at TIMESTAMP NOT NULL
- updated_at TIMESTAMP

### reading_progress
- id_reading_progress UUID PK
- user_id UUID FK users(id_user)
- book_id UUID FK books(id_book)
- current_chapter_id UUID FK chapters(id_chapter)
- progress_percentage DECIMAL(5,2) DEFAULT 0.00
- started_at TIMESTAMP NOT NULL
- last_read_at TIMESTAMP
- finished_at TIMESTAMP
- UNIQUE(user_id, book_id)

### favorites
- id_favorite UUID PK
- user_id UUID FK users(id_user)
- book_id UUID FK books(id_book)
- created_at TIMESTAMP NOT NULL
- UNIQUE(user_id, book_id)

### reviews
- id_review UUID PK
- user_id UUID FK users(id_user)
- book_id UUID FK books(id_book)
- rating INTEGER NOT NULL
- comment TEXT
- created_at TIMESTAMP NOT NULL
- updated_at TIMESTAMP
- UNIQUE(user_id, book_id)
- CHECK rating between 1 and 5

### reports
- id_report UUID PK
- reporter_id UUID FK users(id_user)
- book_id UUID FK books(id_book)
- reason VARCHAR(100) NOT NULL
- description TEXT
- status VARCHAR(30) NOT NULL
- created_at TIMESTAMP NOT NULL
- resolved_at TIMESTAMP

### ai_recommendation_requests
- id_ai_recommendation_request UUID PK
- user_id UUID FK users(id_user)
- query_text TEXT NOT NULL
- mood VARCHAR(40)
- preferred_duration VARCHAR(30)
- preferred_genre VARCHAR(80)
- created_at TIMESTAMP NOT NULL

### ai_recommendation_results
- id_ai_recommendation_result UUID PK
- request_id UUID FK ai_recommendation_requests(id_ai_recommendation_request)
- book_id UUID FK books(id_book)
- match_score INTEGER NOT NULL
- reasons JSONB
- rank_position INTEGER NOT NULL
- UNIQUE(request_id, book_id)
- UNIQUE(request_id, rank_position)

### notifications
- id_notification UUID PK
- user_id UUID FK users(id_user)
- title VARCHAR(150) NOT NULL
- message TEXT NOT NULL
- type VARCHAR(50) NOT NULL
- is_read BOOLEAN DEFAULT FALSE
- created_at TIMESTAMP NOT NULL
- read_at TIMESTAMP
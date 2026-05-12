/**
 * TrackSelector — 2단계 트랙 선택 유틸리티
 *
 * options:
 *   container           HTMLElement       렌더 대상
 *   idPrefix            string            라디오 name 중복 방지용 고유 프리픽스
 *   getCourseTypes      async () => string[]
 *   getTracksByCourseType async (ct) => [{trackId, name}, ...]
 *   onSelect            (trackId: string) => void
 *   compact             boolean           (선택) 대시보드 등 좁은 공간용
 *   initialCourseType   string            (선택) 초기 코스 타입
 *   initialTrackId      string|number     (선택) 초기 트랙 ID
 */
function initTrackSelector(options) {
    const { container, idPrefix, getCourseTypes, getTracksByCourseType, onSelect,
            compact = false, initialCourseType = null, initialTrackId = null } = options;

    let allTracks = [];

    getCourseTypes().then(courseTypes => {
        if (!courseTypes || courseTypes.length === 0) {
            container.innerHTML = '<span style="color:var(--text-muted);font-size:13px;">코스 없음</span>';
            return;
        }

        const pills = courseTypes.map(ct => `
            <label class="ts-pill${compact ? ' ts-pill-sm' : ''}" data-ts-label>
                <input type="radio" name="${_tsEsc(idPrefix)}-ts" value="${_tsEsc(ct)}" data-ts-radio>
                <span>${_tsEsc(ct)}</span>
            </label>`).join('');

        container.innerHTML = `
            <div class="ts-pill-group">${pills}</div>
            <div class="ts-track-row" style="display:none;">
                <input type="text" class="form-control ts-search"
                       placeholder="기수 검색..."
                       style="${compact ? 'font-size:12px;height:32px;padding:4px 8px;' : ''}">
                <select class="form-control ts-select"
                        style="${compact ? 'font-size:12px;height:32px;padding:4px 8px;' : ''}">
                    <option value="">기수 선택</option>
                </select>
            </div>`;

        const trackRow    = container.querySelector('.ts-track-row');
        const searchInput = container.querySelector('.ts-search');
        const trackSelect = container.querySelector('.ts-select');

        async function loadCourseType(ct) {
            trackSelect.innerHTML = '<option value="">불러오는 중...</option>';
            trackRow.style.display = '';
            searchInput.value = '';
            try {
                allTracks = await getTracksByCourseType(ct);
            } catch (e) {
                console.error('기수 로드 실패', e);
                allTracks = [];
            }
            _renderTrackOptions(trackSelect, allTracks);
        }

        // 라디오 변경 → 기수 로드
        container.querySelector('.ts-pill-group').addEventListener('change', async (e) => {
            if (!e.target.matches('[data-ts-radio]')) return;
            _updatePillStyles(container);
            onSelect('');
            await loadCourseType(e.target.value);
        });

        // 검색 → 필터
        searchInput.addEventListener('input', () => {
            const q = searchInput.value.trim().toLowerCase();
            const filtered = q ? allTracks.filter(t => t.name.toLowerCase().includes(q)) : allTracks;
            const cur = trackSelect.value;
            _renderTrackOptions(trackSelect, filtered);
            if (cur && filtered.some(t => String(t.trackId) === cur)) trackSelect.value = cur;
        });

        // 기수 선택 → 콜백
        trackSelect.addEventListener('change', () => onSelect(trackSelect.value));

        // 초기 선택
        if (initialCourseType) {
            const radio = container.querySelector(`[data-ts-radio][value="${CSS.escape(initialCourseType)}"]`);
            if (radio) {
                radio.checked = true;
                _updatePillStyles(container);
                loadCourseType(initialCourseType).then(() => {
                    if (initialTrackId) {
                        trackSelect.value = String(initialTrackId);
                        // 선택 상태이지만 onSelect는 호출하지 않음 (초기화이므로)
                    }
                });
            }
        }
    }).catch(err => {
        console.error('TrackSelector 초기화 실패', err);
        container.innerHTML = '<span style="color:var(--danger);font-size:13px;">트랙 목록 로드 실패</span>';
    });
}

/**
 * 기존 selector의 선택값을 변경 (edit modal 재사용 시)
 * container 안의 라디오/선택 상태를 courseType/trackId로 업데이트하고 onSelect를 호출하지 않음.
 */
function preselectTrackSelector(container, courseType, trackId, getTracksByCourseType) {
    if (!courseType) return;
    const radio = container.querySelector(`[data-ts-radio][value="${CSS.escape(courseType)}"]`);
    if (!radio) return;
    radio.checked = true;
    _updatePillStyles(container);

    const trackRow    = container.querySelector('.ts-track-row');
    const searchInput = container.querySelector('.ts-search');
    const trackSelect = container.querySelector('.ts-select');
    if (!trackRow) return;

    trackSelect.innerHTML = '<option value="">불러오는 중...</option>';
    trackRow.style.display = '';
    if (searchInput) searchInput.value = '';

    getTracksByCourseType(courseType).then(tracks => {
        _renderTrackOptions(trackSelect, tracks);
        if (trackId) trackSelect.value = String(trackId);
    }).catch(() => {
        trackSelect.innerHTML = '<option value="">로드 실패</option>';
    });
}

function _updatePillStyles(container) {
    container.querySelectorAll('[data-ts-label]').forEach(lbl => {
        lbl.classList.toggle('ts-pill-active', lbl.querySelector('[data-ts-radio]').checked);
    });
}

function _renderTrackOptions(select, tracks) {
    select.innerHTML = '<option value="">기수 선택</option>' +
        (tracks || []).map(t => `<option value="${t.trackId}">${_tsEsc(t.name)}</option>`).join('');
}

function _tsEsc(str) {
    return String(str ?? '').replace(/&/g, '&amp;').replace(/</g, '&lt;').replace(/>/g, '&gt;').replace(/"/g, '&quot;');
}

/** API 기반 헬퍼 — admin 페이지에서 공통 사용 */
async function fetchCourseTypes() {
    const r = await fetch('/api/v1/tracks/course-types');
    return (await r.json()).data || [];
}

async function fetchTracksByCourseType(courseType) {
    const r = await fetch(`/api/v1/tracks/by-course-type?courseType=${encodeURIComponent(courseType)}`);
    return (await r.json()).data || [];
}

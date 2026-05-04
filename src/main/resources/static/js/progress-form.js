// ── 드롭다운 연동 ────────────────────────────────────────────────────────────

const courseSelect  = document.getElementById('courseSelect');
const weekSelect    = document.getElementById('weekSelect');
let selectedScore   = null;
let selectedWeekId  = null;

// 강의 선택 → 주차 드롭다운 업데이트
courseSelect.addEventListener('change', function () {
    const courseId = parseInt(this.value);
    weekSelect.innerHTML = '<option value="">주차를 선택하세요</option>';
    weekSelect.disabled  = true;
    selectedWeekId = null;

    if (!courseId) return;

    const course = coursesData.find(c => c.id === courseId);
    if (!course || !course.weeks || course.weeks.length === 0) return;

    course.weeks
        .sort((a, b) => a.weekNumber - b.weekNumber)
        .forEach(w => {
            const opt = document.createElement('option');
            opt.value       = w.id;
            opt.textContent = `${w.weekNumber}주차 (총 ${w.lectureCount}강)`;
            weekSelect.appendChild(opt);
        });

    weekSelect.disabled = false;
});

weekSelect.addEventListener('change', function () {
    selectedWeekId = this.value ? parseInt(this.value) : null;
});

// ── 난이도 버튼 ─────────────────────────────────────────────────────────────

document.querySelectorAll('.score-btn').forEach(btn => {
    btn.addEventListener('click', function () {
        document.querySelectorAll('.score-btn').forEach(b => b.classList.remove('selected'));
        this.classList.add('selected');
        selectedScore = parseInt(this.dataset.score);
    });
});

// ── 제출 처리 ───────────────────────────────────────────────────────────────

document.getElementById('submitBtn').addEventListener('click', async function () {
    const studentName = document.getElementById('studentName').value.trim();
    if (!studentName) { alert('이름을 입력해주세요.'); return; }
    if (!selectedWeekId) { alert('주차를 선택해주세요.'); return; }
    if (!selectedScore)  { alert('학습 난이도를 선택해주세요.'); return; }

    const trackId = location.pathname.split('/').pop();

    const body = {
        studentName:        studentName,
        courseWeekId:       selectedWeekId,
        lectureNumber:      parseInt(document.getElementById('lectureNumber').value) || null,
        status:             document.getElementById('statusSelect').value || null,
        assignmentProgress: parseInt(document.getElementById('assignmentRange').value),
        tilWritten:         document.getElementById('tilCheckbox').checked,
        conditionScore:     selectedScore,
        comment:            document.getElementById('comment').value.trim() || null,
    };

    this.disabled    = true;
    this.textContent = '제출 중...';

    try {
        const res = await fetch(`/progress/${trackId}`, {
            method:  'POST',
            headers: { 'Content-Type': 'application/json' },
            body:    JSON.stringify(body),
        });

        const json = await res.json();

        if (!res.ok) {
            alert(json.message || '제출에 실패했습니다.');
            this.disabled    = false;
            this.textContent = '제출하기';
            return;
        }

        const percent = json.data?.similarStudentPercent ?? 0;
        document.getElementById('resultPercent').textContent = percent + '%';
        document.getElementById('resultText').textContent =
            `지금 나와 비슷한 학습 상황인 수강생이 ${percent}%예요.`;

        const resultCard = document.getElementById('resultCard');
        resultCard.style.display = 'block';
        resultCard.scrollIntoView({ behavior: 'smooth', block: 'center' });

        this.textContent = '제출 완료 ✓';
        this.style.background = '#28A745';

    } catch (e) {
        alert('네트워크 오류가 발생했습니다.');
        this.disabled    = false;
        this.textContent = '제출하기';
    }
});

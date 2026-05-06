// ── 드롭다운 연동 ────────────────────────────────────────────────────────────
// CourseResponse 필드: courseId, name, weeks
// WeekResponse  필드: weekId, weekNumber, lectureCount

const courseSelect = document.getElementById('courseSelect');
const weekSelect   = document.getElementById('weekSelect');
let selectedScore      = null;
let selectedWeekId     = null;
let selectedLectureNum = null;

// 강의 선택 → 주차 드롭다운 업데이트
courseSelect.addEventListener('change', function () {
    const courseId = parseInt(this.value);
    weekSelect.innerHTML = '<option value="">주차를 선택하세요</option>';
    weekSelect.disabled  = true;
    selectedWeekId = null;
    clearLectureButtons();

    if (!courseId) return;

    const course = coursesData.find(c => c.courseId === courseId);
    if (!course || !course.weeks || course.weeks.length === 0) return;

    course.weeks
        .sort((a, b) => a.weekNumber - b.weekNumber)
        .forEach(w => {
            const opt = document.createElement('option');
            opt.value       = w.weekId;
            opt.textContent = `${w.weekNumber}주차 (총 ${w.lectureCount}강)`;
            opt.dataset.lectureCount = w.lectureCount;
            weekSelect.appendChild(opt);
        });

    weekSelect.disabled = false;
});

// 주차 선택 → 강의 번호 버튼 생성
weekSelect.addEventListener('change', function () {
    selectedWeekId = this.value ? parseInt(this.value) : null;
    clearLectureButtons();
    if (!this.value) return;

    const selected = this.options[this.selectedIndex];
    const count    = parseInt(selected.dataset.lectureCount) || 0;
    renderLectureButtons(count);
});

// ── 강의 번호 버튼 ───────────────────────────────────────────────────────────

function renderLectureButtons(count) {
    const container     = document.getElementById('lectureButtons');
    const placeholder   = document.getElementById('lectureButtonsPlaceholder');
    container.innerHTML = '';
    selectedLectureNum  = null;

    if (count <= 0) {
        container.style.display   = 'none';
        placeholder.style.display = 'block';
        return;
    }

    for (let i = 1; i <= count; i++) {
        const btn = document.createElement('button');
        btn.type        = 'button';
        btn.className   = 'score-btn';
        btn.textContent = i + '강';
        btn.dataset.num = i;
        btn.addEventListener('click', function () {
            container.querySelectorAll('.score-btn').forEach(b => b.classList.remove('selected'));
            this.classList.add('selected');
            selectedLectureNum = parseInt(this.dataset.num);
        });
        container.appendChild(btn);
    }

    container.style.display   = 'flex';
    placeholder.style.display = 'none';
}

function clearLectureButtons() {
    const container   = document.getElementById('lectureButtons');
    const placeholder = document.getElementById('lectureButtonsPlaceholder');
    container.innerHTML        = '';
    container.style.display    = 'none';
    placeholder.style.display  = 'block';
    selectedLectureNum = null;
}

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
    if (!studentName)   { alert('이름을 입력해주세요.');         return; }
    if (!selectedWeekId){ alert('주차를 선택해주세요.');         return; }
    if (!selectedScore) { alert('학습 난이도를 선택해주세요.'); return; }

    const trackId = location.pathname.split('/').pop();

    const body = {
        studentName:        studentName,
        courseWeekId:       selectedWeekId,
        lectureNumber:      selectedLectureNum,
        status:             document.getElementById('statusSelect').value || null,
        assignmentProgress: parseInt(document.getElementById('assignmentRange').value),
        tilWritten:         document.getElementById('tilCheckbox').checked,
        conditionScore:     selectedScore,
        comment:            document.getElementById('comment').value.trim() || null,
    };

    this.disabled    = true;
    this.textContent = '제출 중...';

    try {
        const res  = await fetch(`/progress/${trackId}`, {
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

        const data    = json.data ?? {};
        const percent = data.similarStudentPercent ?? 0;
        showResultModal(percent, data.topCases ?? [], data.myCase ?? null);

        this.textContent      = '제출 완료 ✓';
        this.style.background = '#28A745';

    } catch (e) {
        console.error('[progress-form] 제출 오류:', e);
        alert('네트워크 오류가 발생했습니다.');
        this.disabled    = false;
        this.textContent = '제출하기';
    }
});

// ── 결과 모달 ────────────────────────────────────────────────────────────────

let resultChartInstance = null;

function showResultModal(percent, topCases, myCase) {
    // 비율 문구
    document.getElementById('similarText').textContent =
        `지금 나와 비슷한 학습 상황인 수강생이 ${percent}%예요.`;

    // 기존 차트 제거
    if (resultChartInstance) {
        resultChartInstance.destroy();
        resultChartInstance = null;
    }

    // 차트 데이터 구성
    const myScore  = myCase?.conditionScore ?? -1;
    const labels   = topCases.map(c => c.label);
    const counts   = topCases.map(c => c.count);
    const colors   = topCases.map(c =>
        c.conditionScore === myScore ? '#1F4E79' : '#A8C4E0'
    );

    // datalabels 플러그인 전역 등록
    Chart.register(ChartDataLabels);

    const ctx = document.getElementById('resultChart').getContext('2d');
    resultChartInstance = new Chart(ctx, {
        type: 'bar',
        data: {
            labels,
            datasets: [{
                data: counts,
                backgroundColor: colors,
                borderRadius: 6,
                borderSkipped: false,
                barPercentage: 0.5,
                categoryPercentage: 0.8,
            }]
        },
        options: {
            maintainAspectRatio: false,
            plugins: {
                legend: { display: false },
                tooltip: { enabled: false },
                datalabels: {
                    anchor: 'end',
                    align: 'end',
                    offset: 2,
                    font: { size: 13, weight: '700' },
                    color: '#1F4E79',
                    formatter: v => v + '명',
                }
            },
            scales: {
                x: {
                    grid: { display: false },
                    ticks: {
                        autoSkip: false,
                        maxRotation: 0,
                        font: { size: 12 },
                    }
                },
                y: {
                    beginAtZero: true,
                    ticks: {
                        stepSize: 1,
                        precision: 0,
                        font: { size: 11 },
                    },
                    grid: { color: '#F0F4F8' }
                }
            },
            layout: {
                padding: { top: 24 }
            }
        }
    });

    // 모달 표시
    const modal = document.getElementById('resultModal');
    modal.style.display = 'flex';
}

function closeResultModal() {
    document.getElementById('resultModal').style.display = 'none';
}

from docx import Document
from docx.shared import Inches, Pt, RGBColor
from docx.enum.text import WD_ALIGN_PARAGRAPH, WD_BREAK
from docx.enum.section import WD_SECTION
from docx.enum.table import WD_TABLE_ALIGNMENT, WD_CELL_VERTICAL_ALIGNMENT
from docx.oxml import OxmlElement
from docx.oxml.ns import qn
from docx.enum.style import WD_STYLE_TYPE
from pathlib import Path

OUT = Path(r"D:\简历\刘辉荣-织慧通测试开发校招作品集.docx")

# Compact reference guide preset with a restrained technical portfolio override.
PAGE_W, PAGE_H = 11906, 16838  # A4 portrait in DXA
MARGIN = 1080
CONTENT_W = 9360
NAVY = "17324D"
BLUE = "1F5B87"
TEAL = "0F766E"
GOLD = "A66A18"
INK = "1F2937"
MUTED = "667085"
LIGHT = "E8EEF5"
PALE = "F5F8FB"
RISK = "8B2E2E"


def set_cell_shading(cell, fill):
    tc_pr = cell._tc.get_or_add_tcPr()
    shd = tc_pr.find(qn('w:shd'))
    if shd is None:
        shd = OxmlElement('w:shd')
        tc_pr.append(shd)
    shd.set(qn('w:fill'), fill)
    shd.set(qn('w:val'), 'clear')


def set_cell_margins(cell, top=90, start=120, bottom=90, end=120):
    tc = cell._tc
    tc_pr = tc.get_or_add_tcPr()
    tc_mar = tc_pr.first_child_found_in('w:tcMar')
    if tc_mar is None:
        tc_mar = OxmlElement('w:tcMar')
        tc_pr.append(tc_mar)
    for m, v in [('top', top), ('start', start), ('bottom', bottom), ('end', end)]:
        node = tc_mar.find(qn(f'w:{m}'))
        if node is None:
            node = OxmlElement(f'w:{m}')
            tc_mar.append(node)
        node.set(qn('w:w'), str(v))
        node.set(qn('w:type'), 'dxa')


def set_cell_width(cell, width):
    tc_pr = cell._tc.get_or_add_tcPr()
    tc_w = tc_pr.find(qn('w:tcW'))
    if tc_w is None:
        tc_w = OxmlElement('w:tcW')
        tc_pr.append(tc_w)
    tc_w.set(qn('w:w'), str(width))
    tc_w.set(qn('w:type'), 'dxa')


def set_table_geometry(table, widths):
    table.alignment = WD_TABLE_ALIGNMENT.LEFT
    table.autofit = False
    tbl_pr = table._tbl.tblPr
    layout = tbl_pr.find(qn('w:tblLayout'))
    if layout is None:
        layout = OxmlElement('w:tblLayout')
        tbl_pr.append(layout)
    layout.set(qn('w:type'), 'fixed')
    tbl_w = tbl_pr.find(qn('w:tblW'))
    if tbl_w is None:
        tbl_w = OxmlElement('w:tblW')
        tbl_pr.append(tbl_w)
    tbl_w.set(qn('w:w'), str(sum(widths)))
    tbl_w.set(qn('w:type'), 'dxa')
    tbl_ind = tbl_pr.find(qn('w:tblInd'))
    if tbl_ind is None:
        tbl_ind = OxmlElement('w:tblInd')
        tbl_pr.append(tbl_ind)
    tbl_ind.set(qn('w:w'), '120')
    tbl_ind.set(qn('w:type'), 'dxa')
    grid = table._tbl.tblGrid
    for child in list(grid):
        grid.remove(child)
    for width in widths:
        col = OxmlElement('w:gridCol')
        col.set(qn('w:w'), str(width))
        grid.append(col)
    for row in table.rows:
        for idx, cell in enumerate(row.cells):
            set_cell_width(cell, widths[idx])
            set_cell_margins(cell)
            cell.vertical_alignment = WD_CELL_VERTICAL_ALIGNMENT.CENTER


def set_run_font(run, size=10.5, bold=None, color=INK, italic=None):
    run.font.name = '宋体'
    run._element.get_or_add_rPr().rFonts.set(qn('w:eastAsia'), '宋体')
    run._element.get_or_add_rPr().rFonts.set(qn('w:ascii'), '宋体')
    run._element.get_or_add_rPr().rFonts.set(qn('w:hAnsi'), '宋体')
    run.font.size = Pt(size)
    run.font.color.rgb = RGBColor.from_string(color)
    if bold is not None:
        run.bold = bold
    if italic is not None:
        run.italic = italic


def set_para(p, before=0, after=6, line=1.25, align=None, keep=False):
    pf = p.paragraph_format
    pf.space_before = Pt(before)
    pf.space_after = Pt(after)
    pf.line_spacing = line
    if align is not None:
        p.alignment = align
    if keep:
        p.paragraph_format.keep_with_next = True


def add_text(p, text, size=10.5, bold=None, color=INK, italic=None):
    r = p.add_run(text)
    set_run_font(r, size, bold, color, italic)
    return r


def add_p(doc, text='', size=10.5, bold=None, color=INK, italic=None, before=0, after=6, line=1.25, align=None, keep=False):
    p = doc.add_paragraph()
    set_para(p, before, after, line, align, keep)
    if text:
        add_text(p, text, size, bold, color, italic)
    return p


def add_label_value(doc, label, value):
    p = doc.add_paragraph()
    set_para(p, after=4, line=1.2)
    add_text(p, label + '  ', 10, True, BLUE)
    add_text(p, value, 10, False, INK)
    return p


def add_heading(doc, text, level=1, kicker=None):
    p = doc.add_paragraph()
    set_para(p, before=18 if level == 1 else 12, after=8 if level == 1 else 5, line=1.15, keep=True)
    if kicker:
        add_text(p, kicker + '  ', 9, True, GOLD)
    size = 16 if level == 1 else 12.5 if level == 2 else 11.5
    color = NAVY if level == 1 else BLUE if level == 2 else TEAL
    add_text(p, text, size, True, color)
    # subtle bottom rule for H1 only
    if level == 1:
        p_pr = p._p.get_or_add_pPr()
        p_bdr = OxmlElement('w:pBdr')
        bottom = OxmlElement('w:bottom')
        bottom.set(qn('w:val'), 'single')
        bottom.set(qn('w:sz'), '8')
        bottom.set(qn('w:space'), '4')
        bottom.set(qn('w:color'), LIGHT)
        p_bdr.append(bottom)
        p_pr.append(p_bdr)
    return p


def add_bullet(doc, text, level=0, color=INK):
    p = doc.add_paragraph(style='List Bullet' if level == 0 else 'List Bullet 2')
    set_para(p, after=3, line=1.2)
    add_text(p, text, 10.2, False, color)
    return p


def new_numbering_id(doc):
    """Create a fresh decimal numbering instance that restarts at 1."""
    numbering = doc.part.numbering_part.element
    ids = [int(n.get(qn('w:numId'))) for n in numbering.findall(qn('w:num')) if n.get(qn('w:numId'))]
    num_id = max(ids or [0]) + 1
    num = OxmlElement('w:num')
    num.set(qn('w:numId'), str(num_id))
    abs_id = OxmlElement('w:abstractNumId')
    abs_id.set(qn('w:val'), '7')  # built-in decimal ListNumber abstract definition
    num.append(abs_id)
    override = OxmlElement('w:lvlOverride')
    override.set(qn('w:ilvl'), '0')
    start = OxmlElement('w:startOverride')
    start.set(qn('w:val'), '1')
    override.append(start)
    num.append(override)
    numbering.append(num)
    return num_id


def add_number(doc, text, num_id=None):
    p = doc.add_paragraph(style='List Number')
    set_para(p, after=4, line=1.2)
    if num_id is not None:
        p_pr = p._p.get_or_add_pPr()
        num_pr = OxmlElement('w:numPr')
        ilvl = OxmlElement('w:ilvl')
        ilvl.set(qn('w:val'), '0')
        num_id_el = OxmlElement('w:numId')
        num_id_el.set(qn('w:val'), str(num_id))
        num_pr.append(ilvl)
        num_pr.append(num_id_el)
        p_pr.append(num_pr)
    add_text(p, text, 10.2, False, INK)
    return p


def add_callout(doc, label, text, fill=PALE, accent=TEAL):
    table = doc.add_table(rows=1, cols=1)
    set_table_geometry(table, [CONTENT_W])
    cell = table.cell(0, 0)
    set_cell_shading(cell, fill)
    p = cell.paragraphs[0]
    set_para(p, before=1, after=1, line=1.22)
    add_text(p, label + '  ', 10, True, accent)
    add_text(p, text, 10, False, INK)
    doc.add_paragraph().paragraph_format.space_after = Pt(1)
    return table


def add_table(doc, headers, rows, widths, header_fill=LIGHT, font_size=9.4):
    table = doc.add_table(rows=1, cols=len(headers))
    set_table_geometry(table, widths)
    for i, h in enumerate(headers):
        c = table.rows[0].cells[i]
        set_cell_shading(c, header_fill)
        p = c.paragraphs[0]
        set_para(p, after=0, line=1.1, align=WD_ALIGN_PARAGRAPH.CENTER)
        add_text(p, h, font_size, True, NAVY)
    for row in rows:
        cells = table.add_row().cells
        for i, value in enumerate(row):
            p = cells[i].paragraphs[0]
            set_para(p, after=0, line=1.16)
            add_text(p, str(value), font_size, False, INK)
    doc.add_paragraph().paragraph_format.space_after = Pt(1)
    return table


def add_page_break(doc):
    p = doc.add_paragraph()
    p.add_run().add_break(WD_BREAK.PAGE)


def add_header_footer(section):
    section.header_distance = Inches(0.35)
    section.footer_distance = Inches(0.35)
    header = section.header.paragraphs[0]
    header.alignment = WD_ALIGN_PARAGRAPH.RIGHT
    set_para(header, after=0, line=1.0)
    add_text(header, '织慧通 · 测试开发校招作品集', 8.5, False, MUTED)
    footer = section.footer.paragraphs[0]
    footer.alignment = WD_ALIGN_PARAGRAPH.CENTER
    set_para(footer, after=0, line=1.0)
    add_text(footer, '刘辉荣  |  基于项目真实资料整理  |  2026', 8.5, False, MUTED)


def add_cover(doc):
    p = doc.add_paragraph()
    set_para(p, before=66, after=16, line=1.0, align=WD_ALIGN_PARAGRAPH.LEFT)
    add_text(p, 'TEST DEVELOPMENT PORTFOLIO', 10, True, GOLD)
    p = doc.add_paragraph()
    set_para(p, after=8, line=1.0)
    add_text(p, '织慧通', 34, True, NAVY)
    p = doc.add_paragraph()
    set_para(p, after=6, line=1.0)
    add_text(p, '智能纺织生产与质量管理系统', 22, True, BLUE)
    p = doc.add_paragraph()
    set_para(p, after=22, line=1.2)
    add_text(p, '测试开发校招作品集', 16, True, TEAL)
    add_callout(doc, '一句话定位', '围绕订单、生产、质量和 AI 质检链路，建立可回归、可定位、可度量的测试开发体系。', fill='EEF6F5', accent=TEAL)
    add_p(doc, '目标岗位：测试开发 / 软件测试开发 / AI 应用测试', size=11, bold=True, color=INK, before=20, after=5)
    add_p(doc, '项目类型：面向纺织生产现场的 SaaS 管理系统', size=10.5, color=MUTED, after=3)
    add_p(doc, '核心关注：接口自动化、浏览器端 ONNX 推理、质量追溯、权限安全、性能与兼容性', size=10.5, color=MUTED, after=24)
    add_p(doc, '说明：本作品集只使用项目文档中已确认的模块、接口和模型契约；测试结果与后续验收边界会明确区分。', size=9.2, color=MUTED, italic=True, after=0)


def add_navigation(doc):
    add_heading(doc, '阅读导航', 1, '00')
    add_p(doc, '这份作品集按面试官的阅读路径组织：先看项目价值，再看系统结构，最后看我如何把业务风险转化为可执行的测试与自动化方案。', size=10.5, color=INK, after=8)
    rows = [
        ('01', '项目概览', '业务目标、模块边界、技术栈与测试对象'),
        ('02', '系统架构与链路', '浏览器、后端、数据库、模型与文件存储的关系'),
        ('03', '测试开发定位', '测试策略、质量门禁和可维护的测试资产'),
        ('04', '接口与数据测试', '核心接口、状态流转、数据库一致性和权限'),
        ('05', 'AI 质检测试', 'ONNX 契约、Worker、WebGPU/WASM、结果校验与幂等'),
        ('06', '性能与兼容性', '实时推理、证据队列、弱网、浏览器与设备覆盖'),
        ('07', '问题定位案例', '从现象到根因，再到回归用例与工程化修复'),
        ('08', '面试表达', '60 秒项目介绍、追问准备和可继续建设的方向'),
    ]
    add_table(doc, ['编号', '章节', '面试官关注点'], rows, [900, 1900, 6560], font_size=9.5)
    add_callout(doc, '阅读提示', 'AI 在本项目中不是“替代规则”的黑盒：OR-Tools/业务规则负责生成可执行方案，DeepSeek 负责解释与报告；ONNX 负责缺陷识别，后端负责模型与结果的可信校验。', fill='FFF8E8', accent=GOLD)
    add_heading(doc, '我的测试开发主线', 2)
    add_p(doc, '业务链路建模 → 风险分层 → 接口与数据库自动化 → 浏览器 AI 链路专项测试 → 性能/兼容性验证 → 缺陷闭环与质量门禁。', size=11, bold=True, color=NAVY, after=8)
    add_bullet(doc, '优先保证关键链路：订单到排产、来料到 IQC、实时质检到异常闭环。')
    add_bullet(doc, '优先保证可信结果：模型版本、缺陷类别、置信度、坐标边界、图片尺寸都要可校验。')
    add_bullet(doc, '优先保证可回归：接口数据、模型契约、队列状态、权限边界都沉淀为可重复执行的测试资产。')


def add_project_overview(doc):
    add_page_break(doc)
    add_heading(doc, '项目概览', 1, '01')
    add_p(doc, '织慧通是面向纺织生产场景的 SaaS 管理系统，目标是把订单、来料批次、工艺路线、生产计划、质量检验与异常闭环串成一条可追溯链路。系统既处理企业管理数据，也处理浏览器端 AI 质检的模型与证据数据，因此测试重点不是单一页面，而是跨模块状态和结果可信度。', size=10.8, after=8)
    add_table(doc, ['维度', '项目事实', '测试关注'], [
        ('业务对象', '订单、订单明细、批次、工艺路线、设备、生产计划、工序计划、质检记录、异常/CAPA', '状态流转、关联关系、重复提交、越权访问'),
        ('用户角色', '系统用户、角色、菜单、按钮、部门、岗位、在线用户', '认证、权限、数据范围、会话失效'),
        ('AI 场景', 'AI 排产建议、AI 质量分析、浏览器 ONNX 质检', '规则与模型边界、外部大模型超时、结果可解释性'),
        ('技术栈', 'Vue 3 + TypeScript + Vite；Spring Boot 3.3 + Spring Security + MyBatis-Plus；MySQL 8 + Redis；Docker/Nginx', '接口契约、跨端兼容、部署配置、健康检查'),
    ], [1550, 4330, 3480], font_size=9.1)
    add_heading(doc, '模块地图', 2)
    add_p(doc, '系统管理 / 订单管理 / 来料批次 / 工艺与设备 / 生产排产 / 质量管理 / 异常闭环 / AI 能力 / 仪表盘。', size=11, bold=True, color=BLUE, after=8)
    add_callout(doc, '测试边界', '作品集中的“已确认”依据项目 docs：前端类型检查、单元测试、后端编译与测试命令已定义并记录；真实 WebGPU/WASM、摄像头、Docker HTTPS 等目标设备验收仍应在对应环境完成。', fill='FFF4F4', accent=RISK)
    add_heading(doc, '测试开发价值', 2)
    add_bullet(doc, '让业务规则可以被稳定复现：同一订单、批次、工艺与设备组合可以反复验证。')
    add_bullet(doc, '让 AI 结果可以被审计：模型 SHA、metadata 类别、输出维度和坐标边界均有契约。')
    add_bullet(doc, '让线上问题可以被定位：健康检查、Prometheus 指标、证据队列状态和后端日志互相印证。')


def add_architecture(doc):
    add_page_break(doc)
    add_heading(doc, '系统架构与核心业务链路', 1, '02')
    add_heading(doc, '从浏览器到数据落库', 2)
    add_table(doc, ['层次', '组件/路径', '测试切入点'], [
        ('浏览器层', 'Vue 3、Pinia、Axios、路由与权限指令；BrowserInferenceClient + Web Worker', '路由守卫、重复请求、状态清理、Worker 单例与串行推理'),
        ('网关层', 'Nginx /prod-api 代理；COOP、COEP、CORP；静态资源与模型文件', '代理路径、响应头、MJS/WASM MIME、502 与缓存'),
        ('服务层', 'Spring Boot Controller → Service → Mapper；JWT + Redis 会话', '接口契约、异常码、事务、幂等、权限和数据范围'),
        ('数据层', 'MySQL 业务表、Redis 验证码/会话/在线状态、photo/upload 与 photo/results', '关联完整性、状态一致性、存储路径、过期会话'),
    ], [1450, 4620, 3290], font_size=9.1)
    add_heading(doc, '主业务链路', 2)
    add_p(doc, '订单 → 订单明细 → 批次分配 → 来料批次 → 工艺路线 → 生产计划 → 工序计划 → IQC/过程质检 → 异常闭环 / 返工 / CAPA。', size=11, bold=True, color=NAVY, after=8)
    num_id = new_numbering_id(doc)
    add_number(doc, '创建订单和订单明细，确认产品规格、颜色与数量。', num_id)
    add_number(doc, '把来料批次分配给订单明细，进入订单排产池。', num_id)
    add_number(doc, '选择批次、工艺路线和计划时间，生成 productionplan 与 planstep。', num_id)
    add_number(doc, '工序计划绑定设备、起止时间、工时和状态，进入执行与质检。', num_id)
    add_number(doc, '质检结果写入 qcrecord / inspectiondata；异常进入处理、返工和 CAPA。', num_id)
    add_callout(doc, '关键判断', '我会把“跨模块状态是否能回滚、重复提交是否会重复落库、异常关闭是否有权限、质检结果是否能追溯到批次和工序”作为端到端回归的主轴。', fill='EEF6F5', accent=TEAL)


def add_strategy(doc):
    add_page_break(doc)
    add_heading(doc, '测试开发定位与策略', 1, '03')
    add_p(doc, '在这个项目里，我把测试开发工作定义为：用自动化与可观测性把业务风险前移，确保每次代码、模型或配置变更，都能回答“影响了什么、如何复现、是否可回归、上线后如何观察”。', size=10.8, after=8)
    add_heading(doc, '分层测试方案', 2)
    add_table(doc, ['层级', '主要资产', '进入回归的典型内容'], [
        ('单元/组件', 'JUnit、前端推理工具测试、类型检查', '坐标还原、NMS、输出解码、权限判断、状态机边界'),
        ('接口自动化', 'Requests/Pytest 或项目对应 HTTP 客户端、环境配置、断言与报告', '登录、订单、批次、排产、质检、异常、AI 分析接口'),
        ('数据测试', 'MySQL 查询、事务前后置、数据校验脚本', '关联键、状态变化、分页/筛选、幂等记录、结果落库'),
        ('E2E/兼容性', '浏览器 E2E、WebGPU/WASM 开关、摄像头与弱网场景', '图片质检、实时质检、队列积压、模型降级、停止释放资源'),
        ('性能/可观测性', 'JMeter、Prometheus、日志与队列指标', 'QPS、P99、模型首载、推理耗时、证据上传、内存增长'),
    ], [1500, 3180, 4680], font_size=9.1)
    add_heading(doc, '质量门禁', 2)
    add_table(doc, ['门禁', '最低检查', '未通过时的处理'], [
        ('代码门禁', 'frontend: npm test / npm run build；backend: mvn test / mvn package', '阻断合并，先定位类型、单测或编译问题'),
        ('模型门禁', 'manifest 可读、SHA 匹配、metadata 类别连续、输入输出契约一致', '拒绝模型发布，不允许前端静默使用旧模型'),
        ('接口门禁', '鉴权、权限、异常码、关键状态流转、幂等性', '保留请求/响应和数据库证据，回归后再放行'),
        ('环境门禁', 'health、prometheus、Redis、Nginx 代理、文件目录可写', '区分应用缺陷与部署配置问题，形成排错记录'),
    ], [1550, 4890, 2920], font_size=9.1)
    add_callout(doc, '不虚构结果', '项目文档已经定义了测试命令、专项用例与排错手册，但目标设备上的真实 WebGPU/WASM/摄像头/HTTPS 验收需要现场执行；作品集将其写作“验证边界”，不把方案描述成已完成的线上指标。', fill='FFF8E8', accent=GOLD)


def add_api_data(doc):
    add_page_break(doc)
    add_heading(doc, '接口、数据库与权限测试', 1, '04')
    add_heading(doc, '核心接口回归地图', 2)
    add_table(doc, ['域', '代表接口', '关键断言'], [
        ('认证', 'POST /auth/login；GET /auth/me；POST /auth/logout', '验证码、JWT、Redis 会话、过期/注销后访问、密码修改'),
        ('订单/批次', '订单与明细、批次分配、来料批次接口', '必填校验、状态转换、批次归属、重复分配、分页筛选'),
        ('排产', '生产计划、工序计划、甘特图、AI 建议', '时间与工时约束、设备能力、方案确认前不可生效'),
        ('质量', 'POST /api/qc-records/client-detect-results；质检记录查询', '结果字段、模型版本、图片关联、置信度/坐标边界、保存权限'),
        ('实时质检', '创建/事件/关闭 /api/qc-stream-sessions/...', 'session 生命周期、eventId 幂等、重复事件、关闭释放资源'),
        ('AI 模型', 'GET /api/ai/browser-inference/manifest；model?sha256=', 'SHA、ETag/缓存、文件存在性、非法模型拒绝'),
    ], [1450, 4300, 3610], font_size=9.0)
    add_heading(doc, '数据库一致性检查', 2)
    add_bullet(doc, '订单、订单明细、批次分配、生产计划和工序计划之间的外键或业务关联必须可追溯；删除与状态变更要验证级联/禁止规则。')
    add_bullet(doc, '质检记录保存后，原图、结果图、结构化结果和批次/工序上下文必须能通过同一业务主键或事件链查询。')
    add_bullet(doc, '实时事件重复上报时，eventId 作为幂等键；重试不能生成重复质量记录或重复异常。')
    add_bullet(doc, '分页、筛选、排序使用真实数据库结果比对，避免只断言 HTTP 200。')
    add_heading(doc, '权限与安全负向用例', 2)
    add_table(doc, ['场景', '验证方式', '预期'], [
        ('无 token', '直接请求业务接口', '401/统一未认证响应，不能返回业务数据'),
        ('角色无按钮权限', '移除质量保存/模型发布按钮权限后请求接口', '前端隐藏只是体验，后端仍必须拒绝'),
        ('数据范围越权', '用户 A 读取用户 B 部门的订单/质检记录', '按 DataScopeService 约束返回空或无权限'),
        ('上传攻击面', '非法扩展名、超大文件、路径穿越、伪造 content-type', '校验类型/大小/路径，拒绝写入受保护目录'),
    ], [1900, 4200, 3260], font_size=9.2)


def add_ai_testing(doc):
    add_page_break(doc)
    add_heading(doc, 'AI 质检专项测试', 1, '05')
    add_p(doc, 'AI 质检链路由浏览器本地推理和后端可信落库共同完成。测试不能只看页面上有没有框，而要验证模型契约、预处理、推理调度、后处理、结果上传、后端校验和数据追溯是否一致。', size=10.8, after=8)
    add_heading(doc, '模型契约基线', 2)
    add_table(doc, ['项目', '项目文档确认值', '测试动作'], [
        ('输入', 'images，float32 [1, 3, 640, 640]', '检查 dtype、维度、letterbox 与 RGB/归一化处理'),
        ('输出', 'output0，float32 [1, 24, 8400]', '覆盖 BCN/BNC 误读、输出缺失、长度不匹配'),
        ('解码', 'YOLO raw，cxcywh，无 objectness', '验证中心点/宽高还原、类别分数与阈值逻辑'),
        ('类别', '20 类，唯一来源为 ONNX metadata', 'metadata 缺失/不连续时拒绝模型，不使用前端硬编码兜底'),
        ('完整性', 'SHA-256 cd50a08d...c60470', 'manifest、下载 URL、缓存模型与提交结果交叉校验'),
    ], [1500, 3980, 3880], font_size=9.1)
    add_heading(doc, '浏览器 Worker 与结果链路', 2)
    num_id = new_numbering_id(doc)
    add_number(doc, 'BrowserInferenceClient 获取 manifest，校验版本与 SHA 后复用同一 Session。', num_id)
    add_number(doc, 'Worker 优先 WebGPU，失败回退单线程 WASM；图片质检与实时视频共用 Worker，并通过双层队列保证 run() 串行。', num_id)
    add_number(doc, '后处理完成后绘制检测框，连续命中缺陷时按业务规则生成唯一 eventId；普通帧不上传。', num_id)
    add_number(doc, '证据进入有界异步队列，上传原图/结果图/结构化结果；队列满时丢弃较新的待保存证据并提示，不阻塞摄像头推理。', num_id)
    add_number(doc, '后端再次校验模型 SHA、类别、置信度、坐标边界、图片尺寸并幂等落库。', num_id)
    add_heading(doc, '代表性专项用例', 2)
    add_table(doc, ['用例', '输入/操作', '预期断言'], [
        ('坐标还原', '不同宽高图片 + letterbox', '框坐标还原到原图，不能越界或整体偏移'),
        ('NMS', '同一缺陷多个重叠框，不同类别重叠框', '同类抑制、异类保留；阈值边界可重复'),
        ('模型降级', '禁用 WebGPU，加载 WASM；Worker 初始化超时', '状态可见、只重试一次、失败后可重新初始化'),
        ('串行推理', '图片检测与实时检测同时触发', '最大并发为 1，不出现 session.run 并发错误'),
        ('幂等事件', '重复提交同一 eventId，模拟超时重试', '只保留一条事件/质量记录，重试响应可识别'),
        ('资源释放', '停止实时质检、切换路由、关闭摄像头', '释放 camera、Worker、Timer、Tensor，无持续推理或内存增长'),
    ], [1450, 4260, 3650], font_size=9.0)


def add_perf_compat(doc):
    add_page_break(doc)
    add_heading(doc, '性能、弱网与兼容性', 1, '06')
    add_heading(doc, '性能测试设计', 2)
    add_table(doc, ['对象', '场景', '指标/观察'], [
        ('接口', '登录、订单查询、质检结果保存、实时事件上报并发', 'QPS、平均响应、P95/P99、错误率、数据库连接池'),
        ('浏览器模型', '首次加载、同 SHA 二次进入、WebGPU/WASM 回退', '首载耗时、单帧推理耗时、超时与重试次数'),
        ('实时质检', '持续摄像头帧 + 缺陷证据队列', '推理队列长度、queued/uploading/succeeded/failed/dropped、内存趋势'),
        ('大文件/结果图', '上传原图、结果图、结构化 JSON', '上传耗时、超时、重试、存储空间和结果可见性'),
    ], [1500, 4190, 3670], font_size=9.1)
    add_callout(doc, '项目基线', '方案中给出的验收基线包括：模型首次加载目标小于 180 秒；同一时刻只允许一个推理请求；证据队列不能无限增长；连续运行 10 分钟不应出现超时或持续内存增长。它们是验收目标，不等同于已在所有设备上完成的结果。', fill='EEF6F5', accent=TEAL)
    add_heading(doc, '弱网与兼容性矩阵', 2)
    add_table(doc, ['维度', '覆盖', '重点问题'], [
        ('网络', '正常、慢网、断网、恢复、请求超时', '图片降级、结果重试、状态恢复、重复事件、错误提示'),
        ('浏览器', '支持 WebGPU 的 Chromium；禁用 WebGPU 的 WASM 路径', 'COOP/COEP/CORP、WASM MIME、Worker 初始化和兼容降级'),
        ('设备', '中低端设备、摄像头权限变化、前后台切换', 'CPU/内存/耗电、帧积压、摄像头释放、页面卡顿'),
        ('部署', '本地开发、Docker/Nginx、HTTPS/跨域', '模型文件、静态资源、代理路径、健康检查与日志'),
    ], [1400, 3860, 4100], font_size=9.1)
    add_heading(doc, '可观测性检查', 2)
    add_bullet(doc, '服务健康：/actuator/health；指标：/actuator/prometheus；确认敏感端点未暴露。')
    add_bullet(doc, '前端证据队列区分 queued、uploading、succeeded、failed、dropped，定位推理延迟和保存延迟。')
    add_bullet(doc, '排障时关联 request/session/eventId、模型 SHA、文件路径和后端日志，避免只凭页面提示判断。')


def add_case(doc):
    add_page_break(doc)
    add_heading(doc, '问题定位案例：实时质检如何避免“越测越卡”', 1, '07')
    add_p(doc, '这是一个适合面试展开的测试开发案例：实时质检同时涉及摄像头、Worker、模型推理、证据上传和后端幂等，单看某个接口或某个页面都无法定位根因。', size=10.8, after=8)
    add_heading(doc, '现象 → 假设 → 证据 → 修复 → 回归', 2)
    add_table(doc, ['阶段', '我的判断与动作'], [
        ('现象', '连续质检一段时间后出现页面卡顿、上传延迟增大，偶发重复质检记录。'),
        ('假设', '推理 run() 并发、证据队列无界、重试没有幂等、停止时 Worker/Timer/Tensor 未释放。'),
        ('取证', '观察队列状态、sessionId/eventId、浏览器控制台、后端请求日志、Prometheus 指标和数据库重复记录。'),
        ('修复方向', '统一 BrowserInferenceClient；图片与实时共用一个 Session；主线程与 Worker 双层串行；证据队列有界；连续命中生成唯一 eventId；后端幂等保存；关闭时释放资源。'),
        ('回归', '并发触发图片/实时、弱网重试、重复 eventId、切换路由、停止/重启摄像头、禁用 WebGPU、连续运行 10 分钟。'),
    ], [1500, 7860], font_size=9.6)
    add_heading(doc, '为什么这个案例适合测试开发岗位', 2)
    add_bullet(doc, '问题不是“页面样式错了”，而是跨线程、跨网络、跨服务的数据与状态一致性问题。')
    add_bullet(doc, '自动化资产不只是一条接口脚本，还包括模型契约测试、队列状态断言、幂等数据查询和 E2E 场景。')
    add_bullet(doc, '排查结果能沉淀为长期门禁：并发上限、队列上限、超时策略、资源释放和重复事件都可重复验证。')
    add_callout(doc, '面试表达模板', '我先把卡顿拆成推理延迟、排队延迟和上传延迟，再用 sessionId/eventId 和队列指标关联前后端证据；确认并发与无界队列后，把串行推理、有界队列和幂等保存落到代码与回归用例里，最后用弱网、重复事件和 10 分钟稳定性场景验证修复没有引入新问题。', fill='FFF8E8', accent=GOLD)


def add_status(doc):
    add_page_break(doc)
    add_heading(doc, '质量现状、风险边界与后续建设', 1, '08')
    add_heading(doc, '当前资料能支撑的结论', 2)
    add_table(doc, ['结论类型', '可据项目文档确认的内容'], [
        ('测试入口', '前端 npm test、npm run build、npm run test:e2e:onnx；后端 mvn test、mvn package；健康与 Prometheus 检查命令已整理。'),
        ('专项设计', 'ONNX 预处理/后处理、串行队列、超时重置、模型 SHA、后端结果校验、实时 eventId 幂等、资源释放均有明确测试点。'),
        ('工程记录', '浏览器质检链路优化记录中，前端类型检查/单元测试与后端编译/测试通过的记录已存在。'),
        ('验收边界', '真实 WebGPU/WASM、摄像头、Docker HTTPS、目标设备和弱网环境仍需在对应环境完成验收，不能只凭本地脚本下结论。'),
    ], [1700, 7660], font_size=9.4)
    add_heading(doc, '我会继续建设的测试资产', 2)
    add_bullet(doc, '将接口环境、账号、前置数据、模型 SHA 和测试图片参数化，支持本地、测试环境和 CI。')
    add_bullet(doc, '把模型契约做成独立回归集：输入输出维度、类别 metadata、坐标、NMS、阈值和异常输出。')
    add_bullet(doc, '补齐目标设备验收：WebGPU/WASM、摄像头权限、前后台切换、中低端设备资源指标。')
    add_bullet(doc, '在 Jenkins 或同类 CI 中接入 API 回归、前后端构建、报告归档和失败用例重跑。')
    add_bullet(doc, '将线上误报/漏报、超时、重复事件和证据丢弃原因沉淀为可标注、可复现的回归样本。')
    add_callout(doc, '风险声明', '作品集不声称系统已经通过某项软件著作权或生产验收；它展示的是基于真实项目结构形成的测试开发方法、验证边界和可落地的工程化资产。', fill='FFF4F4', accent=RISK)


def add_interview(doc):
    add_page_break(doc)
    add_heading(doc, '面试表达与技能映射', 1, '09')
    add_heading(doc, '60 秒项目介绍', 2)
    add_p(doc, '我参与建设的是织慧通智能纺织生产与质量管理 SaaS，业务覆盖订单、来料批次、工艺路线、生产排产、质量检验和异常闭环。我的测试开发主线是把关键业务链路和浏览器端 AI 质检链路拆成可回归的接口、数据、模型契约和端到端场景：一方面验证 Spring Boot 接口、MySQL/Redis、JWT 权限和状态流转；另一方面重点验证 ONNX Runtime Web 在 WebGPU/WASM 下的模型加载、预处理、后处理、串行 Worker、证据队列、后端结果校验和 eventId 幂等。性能上关注 QPS、P99、推理耗时、队列长度和 10 分钟稳定性；工程上通过构建、单测、E2E、健康检查和指标监控形成质量门禁。', size=10.8, after=10)
    add_heading(doc, '技能点（测试开发方向）', 2)
    add_table(doc, ['方向', '可以在简历/面试中展开的能力'], [
        ('测试分析', '能从业务流程拆分状态、角色、异常、边界和数据依赖，设计功能、接口、数据、性能、兼容性和安全测试。'),
        ('接口自动化', '熟悉 HTTP/JSON、鉴权、参数校验、异常码、分页筛选、幂等和链路前置数据；能用 Python Requests/Pytest 或同类框架沉淀可维护回归脚本。'),
        ('AI 应用测试', '理解模型 manifest、SHA、metadata 类别、输入输出张量、置信度、坐标转换、NMS、WebGPU/WASM 降级和大模型超时兜底。'),
        ('数据与安全', '能用 SQL 校验关联关系、状态一致性、重复数据和结果追溯；理解 JWT、Redis 会话、按钮权限、数据范围和上传安全。'),
        ('性能与稳定性', '能设计并发、P99、模型首载、队列积压、弱网重试、断网恢复、资源释放和长时间运行场景。'),
        ('问题定位', '能结合浏览器控制台、网络请求、后端日志、Prometheus、数据库和 eventId/sessionId 形成证据链。'),
    ], [1550, 7810], font_size=9.3)
    add_heading(doc, '面试官可能追问', 2)
    add_bullet(doc, '为什么排产不让大模型直接决定？——规则/算法/OR-Tools 负责可执行约束，大模型只负责解释和报告，避免不可控输出直接改变生产计划。')
    add_bullet(doc, '如何证明 AI 质检结果可信？——校验模型 SHA、metadata 类别、输入输出契约、置信度和坐标边界，并记录原图、结果图、结构化结果与业务上下文。')
    add_bullet(doc, '如何避免实时检测重复保存？——连续命中生成唯一 eventId，后端按事件幂等；网络重试只允许同一事件更新/复用结果。')
    add_bullet(doc, 'WebGPU 失败怎么办？——显式展示状态，回退单线程 WASM；只重试一次，超时重置 Worker，不能静默卡在“创建推理会话”。')
    add_bullet(doc, '你如何判断是前端还是后端问题？——按时间线关联推理耗时、队列状态、网络请求、后端日志和数据库记录，先定位首次异常发生的层。')
    add_callout(doc, '结束语', '我希望在测试开发岗位上，把“能发现问题”进一步做成“能自动发现、能快速定位、能持续回归”，尤其关注 AI 功能在真实业务链路中的可靠性，而不是只验证页面是否能点通。', fill='EEF6F5', accent=TEAL)


def style_document(doc):
    sec = doc.sections[0]
    sec.page_width = Pt(PAGE_W / 20)
    sec.page_height = Pt(PAGE_H / 20)
    sec.top_margin = Pt(MARGIN / 20)
    sec.bottom_margin = Pt(MARGIN / 20)
    sec.left_margin = Pt(MARGIN / 20)
    sec.right_margin = Pt(MARGIN / 20)
    add_header_footer(sec)
    styles = doc.styles
    normal = styles['Normal']
    normal.font.name = '宋体'
    normal._element.rPr.rFonts.set(qn('w:eastAsia'), '宋体')
    normal.font.size = Pt(10.5)
    normal.font.color.rgb = RGBColor.from_string(INK)
    normal.paragraph_format.space_after = Pt(6)
    normal.paragraph_format.line_spacing = 1.25
    for name in ['List Bullet', 'List Bullet 2', 'List Number']:
        s = styles[name]
        s.font.name = '宋体'
        s._element.rPr.rFonts.set(qn('w:eastAsia'), '宋体')
        s.font.size = Pt(10.2)
        s.font.color.rgb = RGBColor.from_string(INK)
        s.paragraph_format.space_after = Pt(3)
        s.paragraph_format.line_spacing = 1.2


def main():
    OUT.parent.mkdir(parents=True, exist_ok=True)
    doc = Document()
    style_document(doc)
    add_cover(doc)
    add_page_break(doc)
    add_navigation(doc)
    add_project_overview(doc)
    add_architecture(doc)
    add_strategy(doc)
    add_api_data(doc)
    add_ai_testing(doc)
    add_perf_compat(doc)
    add_case(doc)
    add_status(doc)
    add_interview(doc)
    # Apply body font to all existing runs, including table text.
    for p in doc.paragraphs:
        for r in p.runs:
            if not r.font.name:
                set_run_font(r, r.font.size.pt if r.font.size else 10.5, r.bold, r.font.color.rgb.__str__() if r.font.color and r.font.color.type else INK)
    for table in doc.tables:
        for row in table.rows:
            for cell in row.cells:
                for p in cell.paragraphs:
                    for r in p.runs:
                        if not r.font.name:
                            set_run_font(r, r.font.size.pt if r.font.size else 9.5, r.bold, INK)
    doc.core_properties.title = '织慧通智能纺织生产与质量管理系统 - 测试开发校招作品集'
    doc.core_properties.subject = '测试开发校招作品集'
    doc.core_properties.author = '刘辉荣'
    doc.save(OUT)
    print(OUT)


if __name__ == '__main__':
    main()

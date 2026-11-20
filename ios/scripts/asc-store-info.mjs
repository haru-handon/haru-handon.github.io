// App Store Connect에 "하루 한돈" 등록 정보를 채운다 (App Store Connect API).
// 앱 정보(이름·부제·개인정보처리방침·카테고리), 연령 등급, 콘텐츠 권한, 버전 1.0.0의 설명·키워드·프로모션 텍스트·URL·저작권,
// 6.9인치 스크린샷, 빌드 선택, 앱 심사 메모. 값은 ios/store/metadata.json, 스크린샷은 ios/store/screenshots/*.png.
// API로 할 수 없는 것: 앱 개인정보 보호(데이터 수집 항목), 가격, 출시 국가, 심사 연락처 전화번호 → 실행 요약에 안내.
import crypto from 'node:crypto';
import fs from 'node:fs';
import os from 'node:os';
import path from 'node:path';

const ROOT = path.resolve(path.dirname(new URL(import.meta.url).pathname), '..');
const meta = JSON.parse(fs.readFileSync(path.join(ROOT, 'store', 'metadata.json'), 'utf8'));
const BUNDLE = 'com.ohsang.util.haruhandon';
const API = 'https://api.appstoreconnect.apple.com';

const { ASC_KEY_ID: kid, ASC_ISSUER_ID: iss } = process.env;
const key = fs.readFileSync(path.join(os.homedir(), 'private_keys', `AuthKey_${kid}.p8`), 'utf8');
function token() {
  const b64 = (v) => Buffer.from(JSON.stringify(v)).toString('base64url');
  const now = Math.floor(Date.now() / 1000);
  const unsigned = `${b64({ alg: 'ES256', kid, typ: 'JWT' })}.${b64({ iss, iat: now, exp: now + 1100, aud: 'appstoreconnect-v1' })}`;
  const sig = crypto.sign('sha256', Buffer.from(unsigned), { key, dsaEncoding: 'ieee-p1363' }).toString('base64url');
  return `${unsigned}.${sig}`;
}

const done = [];
const todo = [];
const notice = (t, m) => console.log(`::notice title=${t}::${m}`);
const warn = (t, m) => console.log(`::warning title=${t}::${m}`);

async function api(method, p, body) {
  const res = await fetch(p.startsWith('http') ? p : API + p, {
    method,
    headers: { authorization: `Bearer ${token()}`, 'content-type': 'application/json' },
    body: body ? JSON.stringify(body) : undefined,
  });
  const text = await res.text();
  const json = text ? JSON.parse(text) : {};
  if (!res.ok) {
    const e = (json.errors || []).map((x) => `${x.code}: ${x.detail || x.title}`).join(' / ');
    const err = new Error(`${method} ${p} → ${res.status} ${e}`);
    err.status = res.status;
    err.json = json;
    throw err;
  }
  return json;
}

async function step(name, fn) {
  try {
    const r = await fn();
    done.push(name);
    notice(name, r || '완료');
  } catch (e) {
    todo.push(`${name}: ${e.message}`);
    warn(name, e.message.slice(0, 900));
  }
}

/* ---------- 앱 ---------- */
const app = (await api('GET', `/v1/apps?filter[bundleId]=${BUNDLE}`)).data[0];
if (!app) {
  console.log(`::error title=앱 없음::App Store Connect에 ${BUNDLE} 앱이 없어요. 신규 앱을 먼저 만들어 주세요`);
  process.exit(1);
}
notice('앱', `${app.attributes.name} (${app.id})`);

await step('콘텐츠 권한', async () => {
  await api('PATCH', `/v1/apps/${app.id}`, {
    data: { type: 'apps', id: app.id, attributes: { contentRightsDeclaration: 'DOES_NOT_USE_THIRD_PARTY_CONTENT' } },
  });
  return '타사 콘텐츠 없음';
});

/* ---------- 앱 정보 (이름·부제·개인정보처리방침·카테고리·연령 등급) ---------- */
const infos = (await api('GET', `/v1/apps/${app.id}/appInfos`)).data;
const info = infos.find((i) => !['READY_FOR_DISTRIBUTION', 'REPLACED_WITH_NEW_INFO'].includes(i.attributes.appStoreState || i.attributes.state)) || infos[0];

await step('앱 정보 현지화', async () => {
  const locs = (await api('GET', `/v1/appInfos/${info.id}/appInfoLocalizations`)).data;
  const attrs = { name: meta.name, subtitle: meta.subtitle, privacyPolicyUrl: meta.privacyPolicyUrl };
  const ko = locs.find((l) => l.attributes.locale === meta.locale);
  if (ko) {
    await api('PATCH', `/v1/appInfoLocalizations/${ko.id}`, { data: { type: 'appInfoLocalizations', id: ko.id, attributes: attrs } });
  } else {
    await api('POST', '/v1/appInfoLocalizations', {
      data: { type: 'appInfoLocalizations', attributes: { locale: meta.locale, ...attrs },
              relationships: { appInfo: { data: { type: 'appInfos', id: info.id } } } },
    });
  }
  return `이름 "${meta.name}", 부제 "${meta.subtitle}", 개인정보처리방침 URL`;
});

await step('카테고리', async () => {
  await api('PATCH', `/v1/appInfos/${info.id}`, {
    data: { type: 'appInfos', id: info.id, relationships: {
      primaryCategory: { data: { type: 'appCategories', id: meta.primaryCategory } },
      secondaryCategory: { data: { type: 'appCategories', id: meta.secondaryCategory } },
    } },
  });
  return `기본 ${meta.primaryCategory} · 보조 ${meta.secondaryCategory}`;
});

await step('연령 등급', async () => {
  const decl = (await api('GET', `/v1/appInfos/${info.id}/ageRatingDeclaration`)).data;
  const id = decl.id;
  const base = {
    alcoholTobaccoOrDrugUseOrReferences: 'NONE', contests: 'NONE', gamblingSimulated: 'NONE',
    horrorOrFearThemes: 'NONE', matureOrSuggestiveThemes: 'NONE', medicalOrTreatmentInformation: 'NONE',
    profanityOrCrudeHumor: 'NONE', sexualContentGraphicAndNudity: 'NONE', sexualContentOrNudity: 'NONE',
    violenceCartoonOrFantasy: 'NONE', violenceRealistic: 'NONE', violenceRealisticProlongedGraphicOrSadistic: 'NONE',
    gambling: false, unrestrictedWebAccess: false,
  };
  // 2025년 새 질문은 한 번에 같이 보내야 한다. API가 모르는 항목은 빼고, 더 필요한 항목은 채워서 다시 보낸다
  const attrs = {
    ...base,
    lootBox: false, advertising: true, userGeneratedContent: false, messagingAndChat: false,
    healthOrWellnessTopics: false, parentalControls: false, ageAssurance: false, gunsOrOtherWeapons: 'NONE',
  };
  const skipped = [];
  for (let tryNo = 0; tryNo < 6; tryNo++) {
    try {
      await api('PATCH', `/v1/ageRatingDeclarations/${id}`, { data: { type: 'ageRatingDeclarations', id, attributes: attrs } });
      break;
    } catch (e) {
      const errs = (e.json && e.json.errors) || [];
      if (!errs.length || tryNo === 5) throw e;
      for (const x of errs) {
        const name = ((x.source && x.source.pointer) || '').split('/').pop()
          || (/'([A-Za-z]+)'/.exec(x.detail || '') || [])[1];
        if (!name) continue;
        if (/UNKNOWN|NOT_ALLOWED|INVALID/.test(x.code) && name in attrs) {
          delete attrs[name];
          skipped.push(name);
        } else if (/REQUIRED/.test(x.code) && !(name in attrs)) {
          // 모르는 새 질문: 예/아니요면 아니요, 단계형이면 없음
          attrs[name] = /level|Level|Weapons|Themes|Humor|References|Violence/.test(name) ? 'NONE' : false;
        }
      }
    }
  }
  if (skipped.length) todo.push(`연령 등급에서 API가 받지 않은 질문(직접 확인): ${skipped.join(', ')}`);
  return `전부 "없음" (광고 있음)${skipped.length ? ` · API에 없는 질문 ${skipped.length}개는 직접 확인` : ''}`;
});

/* ---------- 버전 1.0.0 ---------- */
const versions = (await api('GET', `/v1/apps/${app.id}/appStoreVersions?filter[platform]=IOS&limit=20`)).data;
let version = versions.find((v) => ['PREPARE_FOR_SUBMISSION', 'DEVELOPER_REJECTED', 'REJECTED', 'METADATA_REJECTED'].includes(v.attributes.appStoreState));
if (!version) {
  const created = await api('POST', '/v1/appStoreVersions', {
    data: { type: 'appStoreVersions', attributes: { platform: 'IOS', versionString: meta.versionString },
            relationships: { app: { data: { type: 'apps', id: app.id } } } },
  });
  version = created.data;
}
notice('버전', `${version.attributes.versionString} (${version.attributes.appStoreState})`);

await step('버전 번호·저작권', async () => {
  await api('PATCH', `/v1/appStoreVersions/${version.id}`, {
    data: { type: 'appStoreVersions', id: version.id, attributes: { versionString: meta.versionString, copyright: meta.copyright } },
  });
  return `${meta.versionString} · © ${meta.copyright}`;
});

let versionLoc;
await step('설명·키워드·프로모션 텍스트', async () => {
  const locs = (await api('GET', `/v1/appStoreVersions/${version.id}/appStoreVersionLocalizations`)).data;
  const attrs = {
    description: meta.description, keywords: meta.keywords, promotionalText: meta.promotionalText,
    supportUrl: meta.supportUrl, marketingUrl: meta.marketingUrl,
  };
  versionLoc = locs.find((l) => l.attributes.locale === meta.locale);
  if (versionLoc) {
    await api('PATCH', `/v1/appStoreVersionLocalizations/${versionLoc.id}`,
              { data: { type: 'appStoreVersionLocalizations', id: versionLoc.id, attributes: attrs } });
  } else {
    versionLoc = (await api('POST', '/v1/appStoreVersionLocalizations', {
      data: { type: 'appStoreVersionLocalizations', attributes: { locale: meta.locale, ...attrs },
              relationships: { appStoreVersion: { data: { type: 'appStoreVersions', id: version.id } } } },
    })).data;
  }
  return `설명 ${meta.description.length}자, 키워드 ${meta.keywords.length}자, 지원·마케팅 URL`;
});

/* ---------- 스크린샷 (6.9인치 1290×2796) ---------- */
await step('스크린샷', async () => {
  if (!versionLoc) throw new Error('버전 현지화가 없어요');
  const dir = path.join(ROOT, 'store', 'screenshots');
  const files = fs.readdirSync(dir).filter((f) => f.endsWith('.png')).sort();
  const sets = (await api('GET', `/v1/appStoreVersionLocalizations/${versionLoc.id}/appScreenshotSets`)).data;
  let set = sets.find((s) => s.attributes.screenshotDisplayType === 'APP_IPHONE_67');
  if (!set) {
    set = (await api('POST', '/v1/appScreenshotSets', {
      data: { type: 'appScreenshotSets', attributes: { screenshotDisplayType: 'APP_IPHONE_67' },
              relationships: { appStoreVersionLocalization: { data: { type: 'appStoreVersionLocalizations', id: versionLoc.id } } } },
    })).data;
  }
  // 예전 것은 지우고 새로 올린다
  const old = (await api('GET', `/v1/appScreenshotSets/${set.id}/appScreenshots`)).data;
  for (const s of old) await api('DELETE', `/v1/appScreenshots/${s.id}`);
  for (const f of files) {
    const buf = fs.readFileSync(path.join(dir, f));
    const shot = (await api('POST', '/v1/appScreenshots', {
      data: { type: 'appScreenshots', attributes: { fileName: f, fileSize: buf.length },
              relationships: { appScreenshotSet: { data: { type: 'appScreenshotSets', id: set.id } } } },
    })).data;
    for (const op of shot.attributes.uploadOperations) {
      const headers = Object.fromEntries((op.requestHeaders || []).map((h) => [h.name, h.value]));
      const r = await fetch(op.url, { method: op.method, headers, body: buf.subarray(op.offset, op.offset + op.length) });
      if (!r.ok) throw new Error(`${f} 업로드 실패 ${r.status}`);
    }
    const md5 = crypto.createHash('md5').update(buf).digest('hex');
    await api('PATCH', `/v1/appScreenshots/${shot.id}`,
              { data: { type: 'appScreenshots', id: shot.id, attributes: { uploaded: true, sourceFileChecksum: md5 } } });
  }
  return `${files.length}장 (iPhone 6.9인치)`;
});

/* ---------- 빌드 ---------- */
await step(`빌드 ${meta.buildNumber} 선택`, async () => {
  const builds = (await api('GET', `/v1/builds?filter[app]=${app.id}&filter[version]=${meta.buildNumber}&filter[preReleaseVersion.version]=${meta.versionString}&limit=5`)).data;
  const b = builds[0];
  if (!b) throw new Error(`빌드 ${meta.buildNumber}이 아직 없어요 (TestFlight 처리 중일 수 있어요)`);
  if (b.attributes.processingState !== 'VALID') throw new Error(`빌드 ${meta.buildNumber} 상태가 ${b.attributes.processingState}예요`);
  await api('PATCH', `/v1/appStoreVersions/${version.id}/relationships/build`, { data: { type: 'builds', id: b.id } });
  return `1.0.0 (${meta.buildNumber})`;
});

/* ---------- 앱 심사 정보 ---------- */
/** 같은 계정의 다른 앱 심사 정보에 적힌 연락처를 찾아 쓴다 (매번 직접 입력하지 않게) */
async function contactFromOtherApps() {
  const apps = (await api('GET', '/v1/apps?limit=50')).data.filter((x) => x.id !== app.id);
  for (const other of apps) {
    const vs = (await api('GET', `/v1/apps/${other.id}/appStoreVersions?limit=5`)).data;
    for (const v of vs) {
      let d = null;
      try { d = (await api('GET', `/v1/appStoreVersions/${v.id}/appStoreReviewDetail`)).data; } catch { d = null; }
      const a = d && d.attributes;
      if (a && a.contactFirstName && a.contactLastName && a.contactEmail && a.contactPhone) {
        return { from: other.attributes.name, contact: {
          contactFirstName: a.contactFirstName, contactLastName: a.contactLastName,
          contactEmail: a.contactEmail, contactPhone: a.contactPhone } };
      }
    }
  }
  return null;
}

await step('앱 심사 정보', async () => {
  let detail = null;
  try {
    detail = (await api('GET', `/v1/appStoreVersions/${version.id}/appStoreReviewDetail`)).data;
  } catch (e) {
    if (e.status !== 404) throw e;
  }
  const cur = (detail && detail.attributes) || {};
  const attrs = { notes: meta.reviewNotes, demoAccountRequired: false };
  let from = '';
  if (!(cur.contactFirstName && cur.contactLastName && cur.contactEmail && cur.contactPhone)) {
    const found = await contactFromOtherApps();
    if (found) {
      Object.assign(attrs, found.contact);
      from = ` · 연락처는 "${found.from}" 심사 정보와 같게`;
    } else {
      todo.push('앱 심사 정보 → 연락처(이름·성·전화번호·이메일)를 직접 입력');
    }
  }
  if (detail) {
    await api('PATCH', `/v1/appStoreReviewDetails/${detail.id}`, { data: { type: 'appStoreReviewDetails', id: detail.id, attributes: attrs } });
  } else {
    await api('POST', '/v1/appStoreReviewDetails', {
      data: { type: 'appStoreReviewDetails', attributes: attrs,
              relationships: { appStoreVersion: { data: { type: 'appStoreVersions', id: version.id } } } },
    });
  }
  return `데모 계정 필요 없음, 영문 메모${from}`;
});

/* ---------- 요약 ---------- */
const summary = [
  '## App Store Connect 등록 정보',
  '',
  '**자동으로 채운 것**',
  ...done.map((d) => `- ${d}`),
  '',
  '**직접 해야 하는 것 (API 없음)**',
  '- 앱 개인정보 보호 → 데이터 수집 항목 (프로젝트 문서 `하루한돈-appstore-등록정보` 표대로)',
  '- 가격 및 사용 가능 여부 → 무료, 출시 국가',
  ...todo.map((t) => `- ${t}`),
];
if (process.env.GITHUB_STEP_SUMMARY) fs.appendFileSync(process.env.GITHUB_STEP_SUMMARY, summary.join('\n') + '\n');
console.log(summary.join('\n'));

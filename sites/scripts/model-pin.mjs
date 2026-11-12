// Read confirmed facts from both legacy and shared-CLI release records.
export function modelPin(record) {
  if (record.module !== 'tfmodel') return null;
  const dependency = record.verification?.required_api_dependencies?.[0];
  const group = dependency?.group ?? record.core_group ?? record.group;
  const artifact = dependency?.artifact ?? record.core_artifact ?? 'core';
  const version = dependency?.version ?? record.core_consumer_version ?? record.core_version;
  if (![group, artifact, version].every(value => typeof value === 'string' && /^[A-Za-z0-9_.~-]+$/.test(value))) {
    throw new Error('Confirmed model release is missing a valid detector pin');
  }
  return `${group}:${artifact}:${version}`;
}

export function modelPinNotice(entry, language) {
  if (entry.module !== 'tfmodel') return '';
  return Object.entries(entry.destinations).map(([destination, record]) => {
    const pin = modelPin(record);
    return language === 'es'
      ? `\n> ${destination}: exporta el detector fijado \`${pin}\`. La versión del modelo es independiente.\n`
      : `\n> ${destination}: exports pinned detector \`${pin}\`. The model version is independent.\n`;
  }).join('');
}

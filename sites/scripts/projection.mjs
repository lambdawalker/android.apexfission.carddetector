/** Card-specific content projections; shared CLI owns rendering, history, links and translation validation. */
export function modelReference(markdown) {
  const lines=markdown.split('\n');
  const sections=[];
  let selected=false,depth=0,fence=false;
  for(const line of lines){
    if(/^\s*```/.test(line))fence=!fence;
    const heading=!fence&&/^(#{2,3})\s+(.+)$/.exec(line);
    if(heading){
      if(heading[1].length<=depth)selected=false;
      if(/`tfmodel\//.test(heading[2])||/\(tfmodel\)/.test(heading[2])){selected=true;depth=heading[1].length;}
    }
    if(selected)sections.push(line);
  }
  if(!sections.length)throw Error('Model API projection found no tfmodel declarations');
  return sections.join('\n').trim()+'\n';
}
export function modelOverview(scope) {
 const es=scope.language==='es';
 return (es?'Este módulo empaqueta el modelo TensorFlow Lite y sus extensiones de catálogo. Su versión es independiente de la del detector.':'This module packages the TensorFlow Lite model and catalog extensions. Its version is independent of the detector.')+`\n\n1. [${es?'Instalación y detector fijado':'Installation and pinned detector'}](${scope.root}installation/)\n2. [${es?'Contrato del modelo':'Model contract'}](${scope.root}model-contract/)\n3. [${es?'API del catálogo':'Catalog API'}](${scope.root}api/detection/)\n\n`+(es?'Selecciona las guías del detector por separado; su versión más reciente puede no coincidir con la dependencia fijada del modelo.':'Select detector guides separately; its latest version may not match the pinned model dependency.')+'\n';
}
export function modelPrerequisite(scope,target) {
 const es=scope.language==='es';
 return es?`> **Este ejemplo requiere el modelo opcional.** El código importa extensiones de \`tfmodel\`; instalar únicamente el detector no incluye esas extensiones ni el modelo. Elige una [versión del modelo](${target}) por separado y revisa el detector fijado que exporta. No añadas a la vez detector y modelo de JitPack: comparten el mismo identificador de artefacto. Para usar solo el detector, proporciona tu propio modelo y catálogo según el [contrato del modelo](${scope.root}model-contract/).\n\n`:`> **This example requires the optional model package.** The code imports \`tfmodel\` extensions; installing only the detector does not include those extensions or the model asset. Choose a [model release](${target}) independently and check its exported detector pin. Do not add both detector and model JitPack dependencies: they share one artifact ID. To use only the detector, provide your own model and catalog following the [model contract](${scope.root}model-contract/).\n\n`;
}

export function insertGuideNotice(markdown,notice) {
 if(!/^# .+$/m.test(markdown))throw Error('Projected raw guide has no title');
 return markdown.replace(/^(# .+\n)/m,'$1\n'+notice);
}

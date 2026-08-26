import json

log_path = r'C:\Users\sherl\.gemini\antigravity\brain\06bd4772-7637-4669-acdc-11f232acd57f\.system_generated\logs\transcript.jsonl'
with open(log_path, 'r', encoding='utf-8', errors='ignore') as f:
    for line in f:
        try:
            data = json.loads(line)
            if 'tool_calls' in data:
                for call in data['tool_calls']:
                    if call['name'] in ['replace_file_content', 'write_to_file']:
                        args = call.get('args', {})
                        target = args.get('TargetFile', '')
                        if 'values\\\\strings.xml' in target or 'values/strings.xml' in target:
                            print(f'--- Step {data.get("step_index")} ---')
                            rep = args.get('ReplacementContent', args.get('CodeContent', ''))
                            if rep:
                                print(rep.encode('utf-8').decode('unicode_escape'))
        except Exception as e:
            pass

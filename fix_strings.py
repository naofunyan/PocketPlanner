import xml.etree.ElementTree as ET
import re

vi_path = 'app/src/main/res/values-vi/strings.xml'
en_path = 'app/src/main/res/values/strings.xml'

vi_tree = ET.parse(vi_path)
en_tree = ET.parse(en_path)

vi_keys = {}
for elem in vi_tree.getroot():
    if 'name' in elem.attrib:
        vi_keys[elem.attrib['name']] = elem.text

en_keys = {}
for elem in en_tree.getroot():
    if 'name' in elem.attrib:
        en_keys[elem.attrib['name']] = elem.text

missing_keys = {k: v for k, v in vi_keys.items() if k not in en_keys}

def guess_english(k, vi_val):
    parts = k.split('_')
    # if it has format specifiers like %1$s, append them
    formats = re.findall(r'%(?:\d\$)?s|%d', str(vi_val))
    
    base = ' '.join([p.capitalize() for p in parts[1:]]) if len(parts) > 1 else k.capitalize()
    
    if formats:
        return base + ' ' + ' '.join(formats)
    return base

with open(en_path, 'r', encoding='utf-8') as f:
    content = f.read()

# Remove the closing tag
content = content.replace('</resources>', '')

new_strings = []
for k, v in missing_keys.items():
    # specialized fixes based on key
    eng = guess_english(k, v)
    if 'name_prefix' in k: eng = f"Name: %1$s"
    elif 'dob_prefix' in k: eng = f"DOB: %1$s"
    elif 'weight_prefix' in k: eng = f"Weight: %1$s"
    elif 'height_prefix' in k: eng = f"Height: %1$s"
    elif 'blood_type_prefix' in k: eng = f"Blood Type: %1$s"
    elif 'allergies_prefix' in k: eng = f"Allergies: %1$s"
    elif 'conditions_prefix' in k: eng = f"Conditions: %1$s"
    elif 'medications_prefix' in k: eng = f"Medications: %1$s"
    elif 'organ_donor_prefix' in k: eng = f"Organ Donor: %1$s"
    elif 'address_prefix' in k: eng = f"Address: %1$s"
    elif 'notes_prefix' in k: eng = f"Notes: %1$s"
    
    # Escape quotes and apostrophes in python string
    eng = eng.replace("'", "\\'")
    
    new_strings.append(f'    <string name="{k}">{eng}</string>')

content += "\n    <!-- Auto-recovered strings -->\n"
content += '\n'.join(new_strings)
content += '\n</resources>'

with open(en_path, 'w', encoding='utf-8') as f:
    f.write(content)

print(f"Added {len(new_strings)} strings.")

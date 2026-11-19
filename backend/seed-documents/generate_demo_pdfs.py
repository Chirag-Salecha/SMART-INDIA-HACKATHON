#!/usr/bin/env python3
"""
Generate demo test PDFs for Bid Compliance Platform verification testing.

Generates:
1. techvision_valid_gst.pdf     - Valid GST for TechVision Solutions Pvt Ltd (GSTIN: 29AABCT1234F1ZX)
2. techvision_valid_pan.pdf     - Valid PAN for TechVision Solutions Pvt Ltd (PAN: AABCT1234F)
3. techvision_valid_udyam.pdf   - Valid Udyam for TechVision Solutions Pvt Ltd (UDYAM-KA-01-0001234)
4. techvision_wrong_gst.pdf     - GST with mismatched entity name (Different Entity name / Meridian GSTIN)
5. techvision_unknown_gst.pdf   - GST with unregistered GSTIN (29XXXXX9999X1Z9 -> NOT_FOUND)
6. techvision_missing_id.pdf    - Document with company name but missing identifier (UNVERIFIABLE)

Can run with PyMuPDF (fitz) or using built-in pure Python PDF generator without dependencies.
"""

import os
import sys

def create_pdf_pure_python(output_path: str, title: str, lines: list[str]):
    """Creates a standard PDF 1.4 file with a text layer using pure Python."""
    content_lines = []
    content_lines.append("BT")
    content_lines.append("/F1 14 Tf")
    content_lines.append("50 720 Td")
    escaped_title = title.replace("\\", "\\\\").replace("(", "\\(").replace(")", "\\)")
    content_lines.append(f"({escaped_title}) Tj")
    content_lines.append("/F1 11 Tf")
    content_lines.append("18 TL")
    content_lines.append("T*")

    for line in lines:
        escaped = line.replace("\\", "\\\\").replace("(", "\\(").replace(")", "\\)")
        content_lines.append(f"({escaped}) '")
    content_lines.append("ET")

    stream_content = "\n".join(content_lines).encode("latin1")
    stream_length = len(stream_content)

    body = []
    offsets = []

    def add_obj(obj_str: str) -> int:
        offset = sum(len(b) for b in body)
        offsets.append(offset)
        body.append(obj_str.encode("latin1"))
        return len(offsets)

    header = b"%PDF-1.4\n%\xe2\xe3\xcf\xd3\n"
    body.append(header)

    add_obj("1 0 obj\n<< /Type /Catalog /Pages 2 0 R >>\nendobj\n")
    add_obj("2 0 obj\n<< /Type /Pages /Kids [3 0 R] /Count 1 >>\nendobj\n")
    add_obj("3 0 obj\n<< /Type /Page /Parent 2 0 R /MediaBox [0 0 612 792] /Contents 4 0 R /Resources << /Font << /F1 5 0 R >> >> >>\nendobj\n")

    offset_4 = sum(len(b) for b in body)
    offsets.append(offset_4)
    stream_obj = f"4 0 obj\n<< /Length {stream_length} >>\nstream\n".encode("latin1") + stream_content + b"\nendstream\nendobj\n"
    body.append(stream_obj)

    add_obj("5 0 obj\n<< /Type /Font /Subtype /Type1 /BaseFont /Helvetica >>\nendobj\n")

    xref_offset = sum(len(b) for b in body)
    xref_lines = [
        f"xref\n0 6\n",
        "0000000000 65535 f \n"
    ]
    for off in offsets:
        xref_lines.append(f"{off:010d} 00000 n \n")
    trailer = (
        f"trailer\n<< /Size 6 /Root 1 0 R >>\n"
        f"startxref\n{xref_offset}\n%%EOF\n"
    )

    full_content = b"".join(body) + "".join(xref_lines).encode("latin1") + trailer.encode("latin1")

    with open(output_path, "wb") as f:
        f.write(full_content)


def create_pdf(output_path: str, title: str, lines: list[str]):
    try:
        import fitz
        doc = fitz.open()
        page = doc.new_page(width=612, height=792)
        all_text = f"{title}\n\n" + "\n".join(lines)
        page.insert_text((50, 72), all_text, fontsize=11)
        doc.save(output_path)
        doc.close()
        print(f"Generated (via PyMuPDF): {output_path}")
    except Exception:
        create_pdf_pure_python(output_path, title, lines)
        print(f"Generated (via Pure Python): {output_path}")


def main():
    target_dir = os.path.dirname(os.path.abspath(__file__))

    documents = {
        "techvision_valid_gst.pdf": {
            "title": "GOVERNMENT OF INDIA - GST REGISTRATION CERTIFICATE",
            "lines": [
                "Legal Name: TechVision Solutions Pvt Ltd",
                "Trade Name: TechVision Solutions",
                "GSTIN: 29AABCT1234F1ZX",
                "Registration Status: Active",
                "Date of Registration: 15/06/2018",
                "Constitution of Business: Private Limited Company",
                "Principal Place of Business: 42, Outer Ring Road, Bellandur, Bengaluru, Karnataka, 560103",
            ],
        },
        "techvision_valid_pan.pdf": {
            "title": "INCOME TAX DEPARTMENT - PERMANENT ACCOUNT NUMBER CARD",
            "lines": [
                "Name: TechVision Solutions Pvt Ltd",
                "Entity Name: TechVision Solutions Pvt Ltd",
                "Permanent Account Number: AABCT1234F",
                "Date of Incorporation: 10/05/2018",
                "Category: Company / Domestic Entity",
                "Status: Active and Validated with ITD Database",
            ],
        },
        "techvision_valid_udyam.pdf": {
            "title": "MINISTRY OF MICRO, SMALL & MEDIUM ENTERPRISES - UDYAM CERTIFICATE",
            "lines": [
                "Udyam Registration Number: UDYAM-KA-01-0001234",
                "Name of Enterprise: TechVision Solutions Pvt Ltd",
                "Enterprise Name: TechVision Solutions Pvt Ltd",
                "Organisation Type: Private Limited",
                "Major Activity: Services",
                "Enterprise Category: Micro Enterprise",
                "Date of Udyam Registration: 20/09/2020",
                "State: Karnataka",
            ],
        },
        "techvision_wrong_gst.pdf": {
            "title": "GOVERNMENT OF INDIA - GST REGISTRATION CERTIFICATE",
            "lines": [
                "Legal Name: Different Entity Solutions Pvt Ltd",
                "Trade Name: Different Entity Solutions",
                "GSTIN: 27ABCDE1234F1Z5",
                "Registration Status: Active",
                "Date of Registration: 01/04/2019",
                "State: Maharashtra",
            ],
        },
        "techvision_unknown_gst.pdf": {
            "title": "GOVERNMENT OF INDIA - GST REGISTRATION CERTIFICATE",
            "lines": [
                "Legal Name: TechVision Solutions Pvt Ltd",
                "Trade Name: TechVision Solutions",
                "GSTIN: 29XXXXX9999X1Z9",
                "Registration Status: Active",
                "Date of Registration: 01/01/2024",
                "State: Karnataka",
            ],
        },
        "techvision_missing_id.pdf": {
            "title": "GENERAL COMMERCIAL COMPLIANCE CERTIFICATE",
            "lines": [
                "Legal Name: TechVision Solutions Pvt Ltd",
                "This certifies that the organization is a registered business entity.",
                "Tax identification numbers have not been endorsed on this document.",
                "Additional proof of registration must be verified separately.",
            ],
        },
    }

    for filename, data in documents.items():
        out_file = os.path.join(target_dir, filename)
        create_pdf(out_file, data["title"], data["lines"])

    print("\nAll 6 test PDFs generated successfully in:", target_dir)


if __name__ == "__main__":
    main()
